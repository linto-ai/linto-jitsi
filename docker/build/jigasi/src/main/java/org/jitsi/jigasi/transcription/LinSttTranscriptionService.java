/*
 * Jigasi, the JItsi GAteway to SIP.
 *
 * Copyright @ 2018 - present 8x8, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.jigasi.transcription;

import java.net.URI;
import java.util.UUID;
import java.util.function.Consumer;

import javax.media.format.AudioFormat;

import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.jitsi.jigasi.JigasiBundleActivator;
import org.jitsi.jigasi.transcription.linagora.SttWebsocketSession;
import org.jitsi.jigasi.transcription.linagora.SttWebsocketStreamingSession;
import org.jitsi.utils.logging.Logger;


/**
 * Implements a TranscriptionService which uses local
 * LinSTT websocket transcription service.
 * <p>
 */
public class LinSttTranscriptionService
    implements TranscriptionService
{

    /**
     * The logger for this class
     */
    private final static Logger logger
            = Logger.getLogger(LinSttTranscriptionService.class);

    /**
     * The URL of the websocket service speech-to-text service.
     */
	public final static String WEBSOCKET_URL_STT_CONFIG 
	= "org.jitsi.jigasi.transcription.linstt.websocket_url";

	public final static String WEBSOCKET_USER_STT_CONFIG 
	= "org.jitsi.jigasi.transcription.linstt.websocket_user";

	public final static String WEBSOCKET_PSW_STT_CONFIG 
	= "org.jitsi.jigasi.transcription.linstt.websocket_password";

	public final static String DEFAULT_WEBSOCKET_URL = "ws://localhost:2700";

	private final String websocketUrl, websocketUser, websocketpassword;

    /**
     * Create a TranscriptionService which will send audio to the google cloud
     * platform to get a transcription
     */
    public LinSttTranscriptionService()
    {
		websocketUrl = JigasiBundleActivator.getConfigurationService()
				.getString(WEBSOCKET_URL_STT_CONFIG, DEFAULT_WEBSOCKET_URL);
        websocketUser = JigasiBundleActivator.getConfigurationService()
				.getString(WEBSOCKET_USER_STT_CONFIG, null);
		websocketpassword = JigasiBundleActivator.getConfigurationService()
				.getString(WEBSOCKET_PSW_STT_CONFIG, null);
    }

    /**
     * No configuration required yet
     */
    public boolean isConfiguredProperly()
    {
        return true;
    }

    /**
     * Sends audio as an array of bytes to LinStt service
     *
     * @param request        the TranscriptionRequest which holds the audio to be sent
     * @param resultConsumer a Consumer which will handle the
     *                       TranscriptionResult
     */
    @Override
    public void sendSingleRequest(final TranscriptionRequest request,
                                  final Consumer<TranscriptionResult> resultConsumer)
    {
        // Try to create the client, which can throw an IOException
        try
        {
            // Set the sampling rate and encoding of the audio
            AudioFormat format = request.getFormat();
            if (!format.getEncoding().equals("LINEAR"))
            {
                throw new IllegalArgumentException("Given AudioFormat" +
                        "has unexpected" +
                        "encoding");
            }

            WebSocketClient ws = new WebSocketClient();
            SttWebsocketSession socket = new SttWebsocketSession(request);
            ws.start();
            ws.connect(socket, new URI(websocketUrl));
            socket.awaitClose();
            
            resultConsumer.accept(
                    new TranscriptionResult(
                            null,
                            UUID.randomUUID(),
                            false,
                            request.getLocale().toLanguageTag(),
                            0,
                            new TranscriptionAlternative(socket.getResult())));

        }
        catch (Exception e)
        {
            logger.error("Error sending single req", e);
        }
    }

    @Override
    public StreamingRecognitionSession initStreamingSession(Participant participant)
        throws UnsupportedOperationException
    {
    	
        try
        {
        	if(websocketUser == null || websocketpassword == null)
        		return new SttWebsocketStreamingSession(participant.getName(), websocketUrl);
        	else
            	return new SttWebsocketStreamingSession(participant.getName()
            		, websocketUrl, websocketUser, websocketpassword);
        }
        catch (Exception e)
        {
            throw new UnsupportedOperationException("Failed to create streaming session", e);
        }
    }

    @Override
    public boolean supportsFragmentTranscription()
    {
        return true;
    }

    @Override
    public boolean supportsStreamRecognition()
    {
        return true;
    }
}

