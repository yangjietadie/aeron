/*
 * Copyright 2017 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.aeron.archive.client;

import io.aeron.ControlledFragmentAssembler;
import io.aeron.Subscription;
import io.aeron.archive.codecs.*;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;

/**
 * Encapsulate the polling, decoding, and dispatching of archive control protocol response messages.
 */
public class ControlResponsePoller
{
    private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
    private final ControlResponseDecoder controlResponseDecoder = new ControlResponseDecoder();
    private final ReplayStartedDecoder replayStartedDecoder = new ReplayStartedDecoder();
    private final ReplayAbortedDecoder replayAbortedDecoder = new ReplayAbortedDecoder();
    private final RecordingDescriptorDecoder recordingDescriptorDecoder = new RecordingDescriptorDecoder();
    private final RecordingUnknownResponseDecoder recordingUnknownResponseDecoder =
        new RecordingUnknownResponseDecoder();

    private final int fragmentLimit;
    private final Subscription subscription;
    private final ControlledFragmentAssembler fragmentAssembler = new ControlledFragmentAssembler(this::onFragment);
    private long correlationId = -1;
    private int templateId = -1;
    private boolean pollComplete = false;

    /**
     * Create a poller for a given subscription to an archive for control response messages.
     *
     * @param subscription  to poll for new events.
     * @param fragmentLimit to apply for each polling operation.
     */
    public ControlResponsePoller(final Subscription subscription, final int fragmentLimit)
    {
        this.subscription = subscription;
        this.fragmentLimit = fragmentLimit;
    }

    /**
     * Poll for recording events and dispatch them to the {@link RecordingEventsListener} for this instance.
     *
     * @return the number of fragments read during the operation. Zero if no events are available.
     */
    public int poll()
    {
        correlationId = -1;
        pollComplete = false;

        return subscription.controlledPoll(fragmentAssembler, fragmentLimit);
    }

    /**
     * Correlation id of the last polled message or -1 if unrecognised template.
     *
     * @return correlation id of the last polled message or -1 if unrecognised template.
     */
    public long correlationId()
    {
        return correlationId;
    }

    /**
     * Has the last polling action received a complete message?
     *
     * @return true of the last polling action received a complete message?
     */
    public boolean isPollComplete()
    {
        return pollComplete;
    }

    /**
     * Get the template id of the last received message.
     *
     * @return the template id of the last received message.
     */
    public int templateId()
    {
        return templateId;
    }

    private ControlledFragmentAssembler.Action onFragment(
        final DirectBuffer buffer,
        final int offset,
        @SuppressWarnings("unused") final int length,
        @SuppressWarnings("unused") final Header header)
    {
        messageHeaderDecoder.wrap(buffer, offset);

        templateId = messageHeaderDecoder.templateId();
        switch (templateId)
        {
            case ControlResponseDecoder.TEMPLATE_ID:
                controlResponseDecoder.wrap(
                    buffer,
                    offset + MessageHeaderEncoder.ENCODED_LENGTH,
                    messageHeaderDecoder.blockLength(),
                    messageHeaderDecoder.version());

                correlationId = controlResponseDecoder.correlationId();
                break;

            case RecordingDescriptorDecoder.TEMPLATE_ID:
                recordingDescriptorDecoder.wrap(
                    buffer,
                    offset + MessageHeaderEncoder.ENCODED_LENGTH,
                    messageHeaderDecoder.blockLength(),
                    messageHeaderDecoder.version());

                correlationId = recordingDescriptorDecoder.correlationId();
                break;

            case ReplayStartedDecoder.TEMPLATE_ID:
                replayStartedDecoder.wrap(
                    buffer,
                    offset + MessageHeaderEncoder.ENCODED_LENGTH,
                    messageHeaderDecoder.blockLength(),
                    messageHeaderDecoder.version());

                correlationId = replayStartedDecoder.correlationId();
                break;

            case ReplayAbortedDecoder.TEMPLATE_ID:
                replayAbortedDecoder.wrap(
                    buffer,
                    offset + MessageHeaderEncoder.ENCODED_LENGTH,
                    messageHeaderDecoder.blockLength(),
                    messageHeaderDecoder.version());

                correlationId = replayAbortedDecoder.correlationId();
                break;

            case RecordingUnknownResponseDecoder.TEMPLATE_ID:
                recordingUnknownResponseDecoder.wrap(
                    buffer,
                    offset + MessageHeaderEncoder.ENCODED_LENGTH,
                    messageHeaderDecoder.blockLength(),
                    messageHeaderDecoder.version());

                correlationId = recordingUnknownResponseDecoder.correlationId();
                break;

            default:
                throw new IllegalStateException("Unknown templateId: " + templateId);
        }

        pollComplete = true;

        return ControlledFragmentAssembler.Action.BREAK;
    }

    public MessageHeaderDecoder messageHeaderDecoder()
    {
        return messageHeaderDecoder;
    }

    public ControlResponseDecoder controlResponseDecoder()
    {
        return controlResponseDecoder;
    }

    public ReplayStartedDecoder replayStartedDecoder()
    {
        return replayStartedDecoder;
    }

    public ReplayAbortedDecoder replayAbortedDecoder()
    {
        return replayAbortedDecoder;
    }

    public RecordingDescriptorDecoder recordingDescriptorDecoder()
    {
        return recordingDescriptorDecoder;
    }

    public RecordingUnknownResponseDecoder recordingUnknownResponseDecoder()
    {
        return recordingUnknownResponseDecoder;
    }
}
