package org.jitsi.jigasi.transcription.linagora;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import javax.media.format.AudioFormat;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.jitsi.jigasi.transcription.TranscriptionRequest;
import org.jitsi.utils.logging.Logger;


/**
 * Session to send websocket data and recieve results. Non-streaming version
 */ @WebSocket
 public class SttWebsocketSession
 {
     private final static Logger logger
     = Logger.getLogger(SttWebsocketSession.class);
	 
	 /* Signal for the end of operation */
     private final CountDownLatch closeLatch;

     /* Request we need to process */
     private final TranscriptionRequest request;
     private final static String EOF_MESSAGE = "{\"eof\" : 1}";

     /* Collect results*/
     private StringBuilder result;

     public SttWebsocketSession(TranscriptionRequest request)
     {
         this.closeLatch = new CountDownLatch(1);
         this.request = request;
         this.result = new StringBuilder();
     }

     @OnWebSocketClose
     public void onClose(int statusCode, String reason)
     {
         this.closeLatch.countDown(); // trigger latch
     }

     @OnWebSocketConnect
     public void onConnect(Session session)
     {
         try
         {
             AudioFormat format = request.getFormat();
             session.getRemote().sendString("{\"config\" : {\"sample_rate\" : " + format.getSampleRate() + "}}");
             ByteBuffer audioBuffer = ByteBuffer.wrap(request.getAudio());
             session.getRemote().sendBytes(audioBuffer);
             session.getRemote().sendString(EOF_MESSAGE);
         }
         catch (IOException e)
         {
             logger.error("Error to transcribe audio", e);
         }
     }

     @OnWebSocketMessage
     public void onMessage(String msg)
     {
         result.append(msg);
         result.append('\n');
     }

     @OnWebSocketError
     public void onError(Throwable cause)
     {
         logger.error("Websocket connection error", cause);
     }

     public String getResult()
     {
         return result.toString();
     }

     public void awaitClose()
         throws InterruptedException
     {
         closeLatch.await();
     }
 }
