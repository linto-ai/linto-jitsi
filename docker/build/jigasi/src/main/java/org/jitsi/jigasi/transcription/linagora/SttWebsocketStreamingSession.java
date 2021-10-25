package org.jitsi.jigasi.transcription.linagora;


import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.jitsi.jigasi.transcription.TranscriptionAlternative;
import org.jitsi.jigasi.transcription.TranscriptionListener;
import org.jitsi.jigasi.transcription.TranscriptionRequest;
import org.jitsi.jigasi.transcription.TranscriptionResult;
import org.jitsi.jigasi.transcription.TranscriptionService.StreamingRecognitionSession;
import org.jitsi.utils.logging.Logger;
import org.json.JSONObject;

/**
 * A Transcription session for transcribing streams, handles
 * the lifecycle of websocket
 */
@WebSocket
public class SttWebsocketStreamingSession implements StreamingRecognitionSession {

	private final static Logger logger
	= Logger.getLogger(SttWebsocketStreamingSession.class);
	private final static String EOF_MESSAGE = "{\"eof\" : 1}";

	private Session session;
	/* The name of the participant */
	private final String debugName;
	/* The sample rate of the audio stream we collect from the first request */
	private double sampleRate = -1.0;
	/* Last returned result so we do not return the same string twice */
	private String lastResult = "";

	private UUID messageID = UUID.randomUUID();
	private static ArrayList<String> transcriptions = new ArrayList<String>();

	/**
	 * List of TranscriptionListeners which will be notified when a
	 * result comes in
	 */
	private final List<TranscriptionListener> listeners = new ArrayList<>();

	public SttWebsocketStreamingSession(String debugName, String host) throws Exception
	{
		this.debugName = debugName;
		ClientUpgradeRequest request = generateConfigClient(host, debugName, null);
		WebSocketClient ws = new WebSocketClient();
		ws.start();
		ws.connect(this, new URI(host), request);
	}

	public SttWebsocketStreamingSession(String debugName, String host, String userName, String password)
			throws Exception
	{
		this.debugName = debugName;
		ClientUpgradeRequest request = generateConfigClient(host, userName, password);
		WebSocketClient ws = new WebSocketClient();
		ws.start();
		ws.connect(this, new URI(host), request);
	}

	public ClientUpgradeRequest generateConfigClient(String host, String username, String password) {
		ClientUpgradeRequest request = new ClientUpgradeRequest();
		request.setHeader(username, host);

		if(password != null)
		{
			byte[] secretBytes = (username+":"+password).getBytes();
			String basicAuthorization = Base64.getEncoder().encodeToString(secretBytes);
			request.setHeader("Authorization", "Basic " + basicAuthorization);
		}

		return request;
	}

	@OnWebSocketClose
	public void onClose(int statusCode, String reason)
	{		
		logger.log(Level.INFO, "WebSocket Closed. Code : " + statusCode + " - "+reason);
		this.session = null;
	}

	@OnWebSocketConnect
	public void onConnect(Session session) throws IOException
	{
		logger.info("WebSocket connected");
		this.session = session;
	}

	@OnWebSocketMessage
	public void onMessage(String message)
	{
		try 
		{
			JSONObject data = new JSONObject(message);
			//logger.info("Msg WS Streaming Server : " + data);

			if (data.has("partial")) 
			{
				if(!lastResult.equals(data.getString("partial")))
				{
					notifyListeners(data.getString("partial"), messageID, true, false);
					transcriptions.add(data.getString("partial"));
				}
				lastResult = data.getString("partial");
			} 
			else if (data.has("text") && !(data.has("words"))) 
			{
				transcriptions.add(data.getString("text"));
				notifyListeners(data.getString("text"),messageID, true, true);
				
				messageID = UUID.randomUUID();
			} 
			else if (data.has("words")) 
			{
				
				transcriptions.add(data.getString("words"));
				notifyListeners(data.getString("words"), messageID, false, true);
				
				messageID = UUID.randomUUID();
				lastResult = "";
				logger.log(Level.INFO, "All transcriptions : " + transcriptions.toString());
			} 
			else if (data.has("eof")) 
			{
				logger.log(Level.INFO, "EOF detected - WebSocket will be close");
			} 
			else logger.log(Level.WARNING, "Unsuported msg");
		}
		catch(Exception e) 
		{
			// Skip, when WS return no transcription result (mainly on user blank) 
			// logger.log(Level.SEVERE, "Not a Json");
		}
	}

	private void notifyListeners(String result, UUID messageID, boolean partial, boolean storeTranscript) {
		double stability;
		if(partial == true) stability = 1.0;
		else stability = 0;

		for (TranscriptionListener l : listeners)
		{
			l.notify(new TranscriptionResult(
					null,
					messageID,
					partial,
					"C",
					stability,
					new TranscriptionAlternative(result)));
			if(storeTranscript == true) 
			{
				l.notify(new TranscriptionResult(
						null,
						messageID,
						false,
						"C",
						1.0,
						new TranscriptionAlternative(result)));
			}
		}
	}
	
	@OnWebSocketError
	public void onError(Throwable cause)
	{
		logger.error("Error while streaming audio data to transcription service" , cause);
	}

	public void sendRequest(TranscriptionRequest request)
	{
		try
		{
			if (sampleRate < 0)
			{
				sampleRate = request.getFormat().getSampleRate();
				session.getRemote()
				.sendString("{\"config\" : {\"sample_rate\" : " + sampleRate + ", \"metadata\":1 }}");
			}

			ByteBuffer audioBuffer = ByteBuffer.wrap(request.getAudio());
			session.getRemote().sendBytes(audioBuffer);
		}
		catch (Exception e)
		{
			logger.error("Error to send websocket request for participant " + debugName, e);
		}
	}

	public void addTranscriptionListener(TranscriptionListener listener)
	{
		listeners.add(listener);
	}

	public void end()
	{
		try
		{
			session.getRemote().sendString(EOF_MESSAGE);
		}
		catch (Exception e)
		{
			logger.error("Error to finalize websocket connection for participant " + debugName, e);
		}
	}

	public boolean ended()
	{
		return session == null;
	}
}
