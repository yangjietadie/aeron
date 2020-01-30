/*
 *  Copyright 2014-2020 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.aeron;

import io.aeron.logbuffer.LogBufferDescriptor;

import static io.aeron.ChannelUri.SPY_QUALIFIER;
import static io.aeron.CommonContext.*;
import static io.aeron.logbuffer.FrameDescriptor.FRAME_ALIGNMENT;
import static io.aeron.logbuffer.LogBufferDescriptor.TERM_MAX_LENGTH;
import static org.agrona.SystemUtil.*;

/**
 * Type safe means of building a channel URI associated with a {@link Publication} or {@link Subscription}.
 *
 * @see Aeron#addPublication(String, int)
 * @see Aeron#addSubscription(String, int)
 * @see ChannelUri
 */
public class ChannelUriStringBuilder
{
    public static final String TAG_PREFIX = "tag:";

    private final StringBuilder sb = new StringBuilder(64);

    private String prefix;
    private String media;
    private String endpoint;
    private String networkInterface;
    private String controlEndpoint;
    private String controlMode;
    private String tags;
    private String alias;
    private String cc;
    private String fc;
    private Boolean reliable;
    private Integer ttl;
    private Integer mtu;
    private Integer termLength;
    private Integer initialTermId;
    private Integer termId;
    private Integer termOffset;
    private Integer sessionId;
    private Long linger;
    private Boolean sparse;
    private Boolean eos;
    private Boolean tether;
    private Boolean group;
    private Boolean rejoin;
    private boolean isSessionIdTagged;

    /**
     * Clear out all the values thus setting back to the initial state.
     *
     * @return this for a fluent API.
     */
    public ChannelUriStringBuilder clear()
    {
        prefix = null;
        media = null;
        endpoint = null;
        networkInterface = null;
        controlEndpoint = null;
        controlMode = null;
        tags = null;
        alias = null;
        cc = null;
        fc = null;
        reliable = null;
        ttl = null;
        mtu = null;
        termLength = null;
        initialTermId = null;
        termId = null;
        termOffset = null;
        sessionId = null;
        linger = null;
        sparse = null;
        eos = null;
        tether = null;
        group = null;
        rejoin = null;
        isSessionIdTagged = false;

        return this;
    }

    /**
     * Validates that the collection of set parameters are valid together.
     *
     * @return this for a fluent API.
     * @throws IllegalStateException if the combination of params is invalid.
     */
    public ChannelUriStringBuilder validate()
    {
        if (null == media)
        {
            throw new IllegalStateException("media type is mandatory");
        }

        if (CommonContext.UDP_MEDIA.equals(media) && (null == endpoint && null == controlEndpoint))
        {
            throw new IllegalStateException("either 'endpoint' or 'control' must be specified for UDP.");
        }

        int count = 0;
        count += null == initialTermId ? 0 : 1;
        count += null == termId ? 0 : 1;
        count += null == termOffset ? 0 : 1;

        if (count > 0)
        {
            if (count < 3)
            {
                throw new IllegalStateException(
                    "if any of then a complete set of 'initialTermId', 'termId', and 'termOffset' must be provided");
            }

            if (termId - initialTermId < 0) // lgtm [java/dereferenced-value-may-be-null]
            {
                throw new IllegalStateException(
                    "difference greater than 2^31 - 1: termId=" + termId + " - initialTermId=" + initialTermId);
            }

            if (null != termLength && termOffset > termLength) // lgtm [java/dereferenced-value-may-be-null]
            {
                throw new IllegalStateException("termOffset=" + termOffset + " > termLength=" + termLength);
            }
        }

        return this;
    }

    /**
     * Set the prefix for taking an addition action such as spying on an outgoing publication with "aeron-spy".
     *
     * @param prefix to be applied to the URI before the the scheme.
     * @return this for a fluent API.
     * @see ChannelUri#SPY_QUALIFIER
     */
    public ChannelUriStringBuilder prefix(final String prefix)
    {
        if (null != prefix && !prefix.equals("") && !prefix.equals(SPY_QUALIFIER))
        {
            throw new IllegalArgumentException("invalid prefix: " + prefix);
        }

        this.prefix = prefix;
        return this;
    }

    /**
     * Set the prefix value to be what is in the {@link ChannelUri}.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see ChannelUri#SPY_QUALIFIER
     */
    public ChannelUriStringBuilder prefix(final ChannelUri channelUri)
    {
        return prefix(channelUri.prefix());
    }

    /**
     * Get the prefix for the additional action to be taken on the request.
     *
     * @return the prefix for the additional action to be taken on the request.
     */
    public String prefix()
    {
        return prefix;
    }

    /**
     * Set the media for this channel. Valid values are "udp" and "ipc".
     *
     * @param media for this channel.
     * @return this for a fluent API.
     */
    public ChannelUriStringBuilder media(final String media)
    {
        switch (media)
        {
            case CommonContext.UDP_MEDIA:
            case CommonContext.IPC_MEDIA:
                break;

            default:
                throw new IllegalArgumentException("invalid media: " + media);
        }

        this.media = media;
        return this;
    }

    /**
     * Set the endpoint value to be what is in the {@link ChannelUri}.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     */
    public ChannelUriStringBuilder media(final ChannelUri channelUri)
    {
        return media(channelUri.media());
    }

    /**
     * The media over which the channel transmits.
     *
     * @return the media over which the channel transmits.
     */
    public String media()
    {
        return media;
    }

    /**
     * Set the endpoint address:port pairing for the channel. This is the address the publication sends to and the
     * address the subscription receives from.
     *
     * @param endpoint address and port for the channel.
     * @return this for a fluent API.
     * @see CommonContext#ENDPOINT_PARAM_NAME
     */
    public ChannelUriStringBuilder endpoint(final String endpoint)
    {
        this.endpoint = endpoint;
        return this;
    }

    /**
     * Set the endpoint value to be what is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#ENDPOINT_PARAM_NAME
     */
    public ChannelUriStringBuilder endpoint(final ChannelUri channelUri)
    {
        return endpoint(channelUri.get(ENDPOINT_PARAM_NAME));
    }

    /**
     * Get the endpoint address:port pairing for the channel.
     *
     * @return the endpoint address:port pairing for the channel.
     * @see CommonContext#ENDPOINT_PARAM_NAME
     */
    public String endpoint()
    {
        return endpoint;
    }

    /**
     * Set the address of the local interface in the form host:[port]/[subnet mask] for routing traffic.
     *
     * @param networkInterface for routing traffic.
     * @return this for a fluent API.
     * @see CommonContext#INTERFACE_PARAM_NAME
     */
    public ChannelUriStringBuilder networkInterface(final String networkInterface)
    {
        this.networkInterface = networkInterface;
        return this;
    }

    /**
     * Set the network interface value to be what is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#INTERFACE_PARAM_NAME
     */
    public ChannelUriStringBuilder networkInterface(final ChannelUri channelUri)
    {
        return networkInterface(channelUri.get(INTERFACE_PARAM_NAME));
    }

    /**
     * Get the address of the local interface in the form host:[port]/[subnet mask] for routing traffic.
     *
     * @return the address of the local interface in the form host:[port]/[subnet mask] for routing traffic.
     * @see CommonContext#INTERFACE_PARAM_NAME
     */
    public String networkInterface()
    {
        return networkInterface;
    }

    /**
     * Set the control address:port pair for dynamically joining a multi-destination-cast publication.
     *
     * @param controlEndpoint for joining a MDC control socket.
     * @return this for a fluent API.
     * @see CommonContext#MDC_CONTROL_PARAM_NAME
     */
    public ChannelUriStringBuilder controlEndpoint(final String controlEndpoint)
    {
        this.controlEndpoint = controlEndpoint;
        return this;
    }

    /**
     * Set the control endpoint value to be what is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#MDC_CONTROL_PARAM_NAME
     */
    public ChannelUriStringBuilder controlEndpoint(final ChannelUri channelUri)
    {
        return controlEndpoint(channelUri.get(MDC_CONTROL_PARAM_NAME));
    }

    /**
     * Get the control address:port pair for dynamically joining a multi-destination-cast publication.
     *
     * @return the control address:port pair for dynamically joining a multi-destination-cast publication.
     * @see CommonContext#MDC_CONTROL_PARAM_NAME
     */
    public String controlEndpoint()
    {
        return controlEndpoint;
    }

    /**
     * Set the control mode for multi-destination-cast. Set to "manual" for allowing control from the publication API.
     *
     * @param controlMode for taking control of MDC.
     * @return this for a fluent API.
     * @see Publication#addDestination(String)
     * @see Publication#removeDestination(String)
     * @see CommonContext#MDC_CONTROL_MODE_PARAM_NAME
     * @see CommonContext#MDC_CONTROL_MODE_MANUAL
     * @see CommonContext#MDC_CONTROL_MODE_DYNAMIC
     */
    public ChannelUriStringBuilder controlMode(final String controlMode)
    {
        if (null != controlMode &&
            !controlMode.equals(CommonContext.MDC_CONTROL_MODE_MANUAL) &&
            !controlMode.equals(CommonContext.MDC_CONTROL_MODE_DYNAMIC))
        {
            throw new IllegalArgumentException("invalid control mode: " + controlMode);
        }

        this.controlMode = controlMode;
        return this;
    }

    /**
     * Set the control mode to be what is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#MDC_CONTROL_MODE_PARAM_NAME
     */
    public ChannelUriStringBuilder controlMode(final ChannelUri channelUri)
    {
        return controlMode(channelUri.get(MDC_CONTROL_MODE_PARAM_NAME));
    }

    /**
     * Get the control mode for multi-destination-cast.
     *
     * @return the control mode for multi-destination-cast.
     * @see CommonContext#MDC_CONTROL_MODE_PARAM_NAME
     * @see CommonContext#MDC_CONTROL_MODE_MANUAL
     * @see CommonContext#MDC_CONTROL_MODE_DYNAMIC
     */
    public String controlMode()
    {
        return controlMode;
    }

    /**
     * Set the subscription semantics for if loss is acceptable, or not, for a reliable message delivery.
     *
     * @param isReliable false if loss can be be gap filled.
     * @return this for a fluent API.
     * @see CommonContext#RELIABLE_STREAM_PARAM_NAME
     */
    public ChannelUriStringBuilder reliable(final Boolean isReliable)
    {
        this.reliable = isReliable;
        return this;
    }

    /**
     * Set the reliable value to be what is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#RELIABLE_STREAM_PARAM_NAME
     */
    public ChannelUriStringBuilder reliable(final ChannelUri channelUri)
    {
        final String reliableStr = channelUri.get(RELIABLE_STREAM_PARAM_NAME);
        if (null == reliableStr)
        {
            reliable = null;
            return this;
        }
        else
        {
            return reliable(Boolean.valueOf(reliableStr));
        }
    }

    /**
     * Get the subscription semantics for if loss is acceptable, or not, for a reliable message delivery.
     *
     * @return the subscription semantics for if loss is acceptable, or not, for a reliable message delivery.
     * @see CommonContext#RELIABLE_STREAM_PARAM_NAME
     */
    public Boolean reliable()
    {
        return reliable;
    }

    /**
     * Set the Time To Live (TTL) for a multicast datagram. Valid values are 0-255 for the number of hops the datagram
     * can progress along.
     *
     * @param ttl value for a multicast datagram.
     * @return this for a fluent API.
     * @see CommonContext#TTL_PARAM_NAME
     */
    public ChannelUriStringBuilder ttl(final Integer ttl)
    {
        if (null != ttl && (ttl < 0 || ttl > 255))
        {
            throw new IllegalArgumentException("TTL not in range 0-255: " + ttl);
        }

        this.ttl = ttl;
        return this;
    }

    /**
     * Set the ttl value to be what is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#TTL_PARAM_NAME
     */
    public ChannelUriStringBuilder ttl(final ChannelUri channelUri)
    {
        final String ttlStr = channelUri.get(TTL_PARAM_NAME);
        if (null == ttlStr)
        {
            ttl = null;
            return this;
        }
        else
        {
            return ttl(Integer.valueOf(ttlStr));
        }
    }

    /**
     * Get the Time To Live (TTL) for a multicast datagram.
     *
     * @return the Time To Live (TTL) for a multicast datagram.
     * @see CommonContext#TTL_PARAM_NAME
     */
    public Integer ttl()
    {
        return ttl;
    }

    /**
     * Set the maximum transmission unit (MTU) including Aeron header for a datagram payload. If this is greater
     * than the network MTU for UDP then the packet will be fragmented and can amplify the impact of loss.
     *
     * @param mtu the maximum transmission unit including Aeron header for a datagram payload.
     * @return this for a fluent API.
     * @see CommonContext#MTU_LENGTH_PARAM_NAME
     */
    public ChannelUriStringBuilder mtu(final Integer mtu)
    {
        if (null != mtu)
        {
            if (mtu < 32 || mtu > 65504)
            {
                throw new IllegalArgumentException("MTU not in range 32-65504: " + mtu);
            }

            if ((mtu & (FRAME_ALIGNMENT - 1)) != 0)
            {
                throw new IllegalArgumentException("MTU not a multiple of FRAME_ALIGNMENT: mtu=" + mtu);
            }
        }

        this.mtu = mtu;
        return this;
    }

    /**
     * Set the mtu value to be what is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#MTU_LENGTH_PARAM_NAME
     */
    public ChannelUriStringBuilder mtu(final ChannelUri channelUri)
    {
        final String mtuStr = channelUri.get(MTU_LENGTH_PARAM_NAME);
        if (null == mtuStr)
        {
            mtu = null;
            return this;
        }
        else
        {
            final long value = parseSize(MTU_LENGTH_PARAM_NAME, mtuStr);
            if (value > Integer.MAX_VALUE)
            {
                throw new IllegalStateException(MTU_LENGTH_PARAM_NAME + " " + value + " > " + Integer.MAX_VALUE);
            }

            return mtu((int)value);
        }
    }

    /**
     * Get the maximum transmission unit (MTU) including Aeron header for a datagram payload. If this is greater
     * than the network MTU for UDP then the packet will be fragmented and can amplify the impact of loss.
     *
     * @return the maximum transmission unit (MTU) including Aeron header for a datagram payload.
     * @see CommonContext#MTU_LENGTH_PARAM_NAME
     */
    public Integer mtu()
    {
        return mtu;
    }

    /**
     * Set the length of buffer used for each term of the log. Valid values are powers of 2 in the 64K - 1G range.
     *
     * @param termLength of the buffer used for each term of the log.
     * @return this for a fluent API.
     * @see CommonContext#TERM_LENGTH_PARAM_NAME
     */
    public ChannelUriStringBuilder termLength(final Integer termLength)
    {
        if (null != termLength)
        {
            LogBufferDescriptor.checkTermLength(termLength);
        }

        this.termLength = termLength;
        return this;
    }

    /**
     * Set the termLength value to be what is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#TERM_LENGTH_PARAM_NAME
     */
    public ChannelUriStringBuilder termLength(final ChannelUri channelUri)
    {
        final String termLengthStr = channelUri.get(TERM_LENGTH_PARAM_NAME);
        if (null == termLengthStr)
        {
            termLength = null;
            return this;
        }
        else
        {
            final long value = parseSize(TERM_LENGTH_PARAM_NAME, termLengthStr);
            if (value > Integer.MAX_VALUE)
            {
                throw new IllegalStateException(
                    "Term length more than max length of " + TERM_MAX_LENGTH + ": length=" + termLength);
            }

            return termLength((int)value);
        }
    }

    /**
     * Get the length of buffer used for each term of the log.
     *
     * @return the length of buffer used for each term of the log.
     * @see CommonContext#TERM_LENGTH_PARAM_NAME
     */
    public Integer termLength()
    {
        return termLength;
    }

    /**
     * Set the initial term id at which a publication will start.
     *
     * @param initialTermId the initial term id at which a publication will start.
     * @return this for a fluent API.
     * @see CommonContext#INITIAL_TERM_ID_PARAM_NAME
     */
    public ChannelUriStringBuilder initialTermId(final Integer initialTermId)
    {
        this.initialTermId = initialTermId;
        return this;
    }

    /**
     * Set the initialTermId value to be what is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#INITIAL_TERM_ID_PARAM_NAME
     */
    public ChannelUriStringBuilder initialTermId(final ChannelUri channelUri)
    {
        final String termLengthStr = channelUri.get(INITIAL_TERM_ID_PARAM_NAME);
        if (null == termLengthStr)
        {
            initialTermId = null;
            return this;
        }
        else
        {
            return initialTermId(Integer.valueOf(termLengthStr));
        }
    }

    /**
     * the initial term id at which a publication will start.
     *
     * @return the initial term id at which a publication will start.
     * @see CommonContext#INITIAL_TERM_ID_PARAM_NAME
     */
    public Integer initialTermId()
    {
        return initialTermId;
    }

    /**
     * Set the current term id at which a publication will start. This when combined with the initial term can
     * establish a starting position.
     *
     * @param termId at which a publication will start.
     * @return this for a fluent API.
     * @see CommonContext#TERM_ID_PARAM_NAME
     */
    public ChannelUriStringBuilder termId(final Integer termId)
    {
        this.termId = termId;
        return this;
    }

    /**
     * Set the termId value to be what is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#TERM_ID_PARAM_NAME
     */
    public ChannelUriStringBuilder termId(final ChannelUri channelUri)
    {
        final String termIdStr = channelUri.get(TERM_ID_PARAM_NAME);
        if (null == termIdStr)
        {
            termId = null;
            return this;
        }
        else
        {
            return termId(Integer.valueOf(termIdStr));
        }
    }

    /**
     * Get the current term id at which a publication will start.
     *
     * @return the current term id at which a publication will start.
     * @see CommonContext#TERM_ID_PARAM_NAME
     */
    public Integer termId()
    {
        return termId;
    }

    /**
     * Set the offset within a term at which a publication will start. This when combined with the term id can establish
     * a starting position.
     *
     * @param termOffset within a term at which a publication will start.
     * @return this for a fluent API.
     * @see CommonContext#TERM_OFFSET_PARAM_NAME
     */
    public ChannelUriStringBuilder termOffset(final Integer termOffset)
    {
        if (null != termOffset)
        {
            if ((termOffset < 0 || termOffset > TERM_MAX_LENGTH))
            {
                throw new IllegalArgumentException("term offset not in range 0-1g: " + termOffset);
            }

            if (0 != (termOffset & (FRAME_ALIGNMENT - 1)))
            {
                throw new IllegalArgumentException("term offset not multiple of FRAME_ALIGNMENT: " + termOffset);
            }
        }

        this.termOffset = termOffset;
        return this;
    }

    /**
     * Set the termOffset value to be what is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#TERM_OFFSET_PARAM_NAME
     */
    public ChannelUriStringBuilder termOffset(final ChannelUri channelUri)
    {
        final String termOffsetStr = channelUri.get(TERM_OFFSET_PARAM_NAME);
        if (null == termOffsetStr)
        {
            termOffset = null;
            return this;
        }
        else
        {
            return termOffset(Integer.valueOf(termOffsetStr));
        }
    }

    /**
     * Get the offset within a term at which a publication will start.
     *
     * @return the offset within a term at which a publication will start.
     * @see CommonContext#TERM_OFFSET_PARAM_NAME
     */
    public Integer termOffset()
    {
        return termOffset;
    }

    /**
     * Set the session id for a publication or restricted subscription.
     *
     * @param sessionId for the publication or a restricted subscription.
     * @return this for a fluent API.
     * @see CommonContext#SESSION_ID_PARAM_NAME
     */
    public ChannelUriStringBuilder sessionId(final Integer sessionId)
    {
        this.sessionId = sessionId;
        return this;
    }

    /**
     * Set the sessionId value to be what is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#SESSION_ID_PARAM_NAME
     */
    public ChannelUriStringBuilder sessionId(final ChannelUri channelUri)
    {
        final String sessionIdStr = channelUri.get(SESSION_ID_PARAM_NAME);
        if (null == sessionIdStr)
        {
            sessionId = null;
            return this;
        }
        else
        {
            return sessionId(Integer.valueOf(sessionIdStr));
        }
    }

    /**
     * Get the session id for a publication or restricted subscription.
     *
     * @return the session id for a publication or restricted subscription.
     * @see CommonContext#SESSION_ID_PARAM_NAME
     */
    public Integer sessionId()
    {
        return sessionId;
    }

    /**
     * Set the time a network publication will linger in nanoseconds after being drained. This time is so that tail loss
     * can be recovered.
     *
     * @param lingerNs time for the publication after it is drained.
     * @return this for a fluent API.
     * @see CommonContext#LINGER_PARAM_NAME
     */
    public ChannelUriStringBuilder linger(final Long lingerNs)
    {
        if (null != lingerNs && lingerNs < 0)
        {
            throw new IllegalArgumentException("linger value cannot be negative: " + lingerNs);
        }

        this.linger = lingerNs;
        return this;
    }

    /**
     * Set the linger value to be what is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#LINGER_PARAM_NAME
     */
    public ChannelUriStringBuilder linger(final ChannelUri channelUri)
    {
        final String lingerStr = channelUri.get(LINGER_PARAM_NAME);
        if (null == lingerStr)
        {
            linger = null;
            return this;
        }
        else
        {
            return linger(parseDuration(LINGER_PARAM_NAME, lingerStr));
        }
    }

    /**
     * Get the time a network publication will linger in nanoseconds after being drained. This time is so that tail loss
     * can be recovered.
     *
     * @return the linger time in nanoseconds a publication will wait around after being drained.
     * @see CommonContext#LINGER_PARAM_NAME
     */
    public Long linger()
    {
        return linger;
    }

    /**
     * Set to indicate if a term log buffer should be sparse on disk or not. Sparse saves space at the potential
     * expense of latency.
     *
     * @param isSparse true if the term buffer log is sparse on disk.
     * @return this for a fluent API.
     * @see CommonContext#SPARSE_PARAM_NAME
     */
    public ChannelUriStringBuilder sparse(final Boolean isSparse)
    {
        this.sparse = isSparse;
        return this;
    }

    /**
     * Set the sparse value to be what is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#SPARSE_PARAM_NAME
     */
    public ChannelUriStringBuilder sparse(final ChannelUri channelUri)
    {
        final String sparseStr = channelUri.get(SPARSE_PARAM_NAME);
        if (null == sparseStr)
        {
            sparse = null;
            return this;
        }
        else
        {
            return sparse(Boolean.valueOf(sparseStr));
        }
    }

    /**
     * Should term log buffer be sparse on disk or not. Sparse saves space at the potential expense of latency.
     *
     * @return true if the term buffer log is sparse on disk.
     * @see CommonContext#SPARSE_PARAM_NAME
     */
    public Boolean sparse()
    {
        return sparse;
    }

    /**
     * Set to indicate if an EOS should be sent on the media or not.
     *
     * @param eos true if the EOS should be sent.
     * @return this for a fluent API.
     * @see CommonContext#EOS_PARAM_NAME
     */
    public ChannelUriStringBuilder eos(final Boolean eos)
    {
        this.eos = eos;
        return this;
    }

    /**
     * Set the eos value to be what is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#EOS_PARAM_NAME
     */
    public ChannelUriStringBuilder eos(final ChannelUri channelUri)
    {
        final String eosStr = channelUri.get(EOS_PARAM_NAME);
        if (null == eosStr)
        {
            eos = null;
            return this;
        }
        else
        {
            return eos(Boolean.valueOf(eosStr));
        }
    }

    /**
     * Should an EOS flag be sent on the media or not.
     *
     * @return true if the EOS param should be set.
     * @see CommonContext#EOS_PARAM_NAME
     */
    public Boolean eos()
    {
        return eos;
    }

    /**
     * Should the subscription channel be tethered or not for local flow control.
     *
     * @param tether value to be set for the tether param.
     * @return this for a fluent API.
     * @see CommonContext#TETHER_PARAM_NAME
     */
    public ChannelUriStringBuilder tether(final Boolean tether)
    {
        this.tether = tether;
        return this;
    }

    /**
     * Set the tether value to be what is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#TETHER_PARAM_NAME
     */
    public ChannelUriStringBuilder tether(final ChannelUri channelUri)
    {
        final String tetherStr = channelUri.get(TETHER_PARAM_NAME);
        if (null == tetherStr)
        {
            tether = null;
            return this;
        }
        else
        {
            return tether(Boolean.valueOf(tetherStr));
        }
    }

    /**
     * Should the subscription channel be tethered or not for local flow control.
     *
     * @return value of the tether param.
     * @see CommonContext#TETHER_PARAM_NAME
     */
    public Boolean tether()
    {
        return tether;
    }

    /**
     * Is the receiver likely to be part of a group. This informs behaviour such as loss handling.
     *
     * @param group value to be set for the group param.
     * @return this for a fluent API.
     * @see CommonContext#GROUP_PARAM_NAME
     * @see #controlMode()
     * @see #controlEndpoint()
     */
    public ChannelUriStringBuilder group(final Boolean group)
    {
        this.group = group;
        return this;
    }

    /**
     * Set the group value to be what is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#GROUP_PARAM_NAME
     */
    public ChannelUriStringBuilder group(final ChannelUri channelUri)
    {
        final String groupStr = channelUri.get(GROUP_PARAM_NAME);
        if (null == groupStr)
        {
            group = null;
            return this;
        }
        else
        {
            return group(Boolean.valueOf(groupStr));
        }
    }

    /**
     * Is the receiver likely to be part of a group. This informs behaviour such as loss handling.
     *
     * @return value of the group param.
     * @see CommonContext#GROUP_PARAM_NAME
     * @see #controlMode()
     * @see #controlEndpoint()
     */
    public Boolean group()
    {
        return group;
    }

    /**
     * Set the tags for a channel used by a publication or subscription. Tags can be used to identify or tag a
     * channel so that a configuration can be referenced and reused.
     *
     * @param tags for the channel, publication or subscription.
     * @return this for a fluent API.
     * @see CommonContext#TAGS_PARAM_NAME
     * @see CommonContext#TAG_PREFIX
     */
    public ChannelUriStringBuilder tags(final String tags)
    {
        this.tags = tags;
        return this;
    }

    /**
     * Set the tags to be value which is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#TAGS_PARAM_NAME
     */
    public ChannelUriStringBuilder tags(final ChannelUri channelUri)
    {
        return tags(channelUri.get(TAGS_PARAM_NAME));
    }

    /**
     * Get the tags for a channel used by a publication or subscription. Tags can be used to identify or tag a
     * channel so that a configuration can be referenced and reused.
     *
     * @return the tags for a channel, publication or subscription.
     * @see CommonContext#TAGS_PARAM_NAME
     * @see CommonContext#TAG_PREFIX
     */
    public String tags()
    {
        return tags;
    }

    /**
     * Toggle the value for {@link #sessionId()} being tagged or not.
     *
     * @param isSessionIdTagged for session id
     * @return this for a fluent API.
     * @see CommonContext#TAGS_PARAM_NAME
     * @see CommonContext#TAG_PREFIX
     */
    public ChannelUriStringBuilder isSessionIdTagged(final boolean isSessionIdTagged)
    {
        this.isSessionIdTagged = isSessionIdTagged;
        return this;
    }

    /**
     * Is the value for {@link #sessionId()} a tagged.
     *
     * @return whether the value for {@link #sessionId()} a tag reference or not.
     * @see CommonContext#TAGS_PARAM_NAME
     * @see CommonContext#TAG_PREFIX
     */
    public boolean isSessionIdTagged()
    {
        return isSessionIdTagged;
    }

    /**
     * Set the alias for a URI. Alias's are not interpreted by Aeron and are to be used by the application
     *
     * @param alias for the URI.
     * @return this for a fluent API.
     * @see CommonContext#ALIAS_PARAM_NAME
     */
    public ChannelUriStringBuilder alias(final String alias)
    {
        this.alias = alias;
        return this;
    }

    /**
     * Set the alias to be value which is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#TAGS_PARAM_NAME
     */
    public ChannelUriStringBuilder alias(final ChannelUri channelUri)
    {
        return alias(channelUri.get(ALIAS_PARAM_NAME));
    }

    /**
     * Get the alias present in the URI.
     *
     * @return alias for the URI.
     * @see CommonContext#ALIAS_PARAM_NAME
     */
    public String alias()
    {
        return alias;
    }

    /**
     * Set the congestion control algorithm to be used on a channel.
     *
     * @param congestionControl for the URI.
     * @return this for a fluent API.
     * @see CommonContext#CONGESTION_CONTROL_PARAM_NAME
     */
    public ChannelUriStringBuilder congestionControl(final String congestionControl)
    {
        this.cc = congestionControl;
        return this;
    }

    /**
     * Set the congestion control to be value which is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#CONGESTION_CONTROL_PARAM_NAME
     */
    public ChannelUriStringBuilder congestionControl(final ChannelUri channelUri)
    {
        return congestionControl(channelUri.get(CONGESTION_CONTROL_PARAM_NAME));
    }

    /**
     * Get the congestion control algorithm to be used on a channel.
     *
     * @return alias for the URI.
     * @see CommonContext#CONGESTION_CONTROL_PARAM_NAME
     */
    public String congestionControl()
    {
        return cc;
    }

    /**
     * Set the flow control strategy to be used on a channel.
     *
     * @param flowControl for the URI.
     * @return this for a fluent API.
     * @see CommonContext#FLOW_CONTROL_PARAM_NAME
     */
    public ChannelUriStringBuilder flowControl(final String flowControl)
    {
        this.fc = flowControl;
        return this;
    }

    /**
     * Set the flow control to be value which is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#FLOW_CONTROL_PARAM_NAME
     */
    public ChannelUriStringBuilder flowControl(final ChannelUri channelUri)
    {
        return flowControl(channelUri.get(FLOW_CONTROL_PARAM_NAME));
    }

    /**
     * Get the flow control strategy to be used on a channel.
     *
     * @return alias for the URI.
     * @see CommonContext#FLOW_CONTROL_PARAM_NAME
     */
    public String flowControl()
    {
        return fc;
    }

    /**
     * Set the subscription semantics for if a stream should be rejoined after going unavailable.
     *
     * @param rejoin false if stream is not to be rejoined.
     * @return this for a fluent API.
     * @see CommonContext#REJOIN_PARAM_NAME
     */
    public ChannelUriStringBuilder rejoin(final Boolean rejoin)
    {
        this.rejoin = rejoin;
        return this;
    }

    /**
     * Set the rejoin value to be what is in the {@link ChannelUri} which may be null.
     *
     * @param channelUri to read the value from.
     * @return this for a fluent API.
     * @see CommonContext#REJOIN_PARAM_NAME
     */
    public ChannelUriStringBuilder rejoin(final ChannelUri channelUri)
    {
        final String rejoinStr = channelUri.get(REJOIN_PARAM_NAME);
        if (null == rejoinStr)
        {
            rejoin = null;
            return this;
        }
        else
        {
            return rejoin(Boolean.valueOf(rejoinStr));
        }
    }

    /**
     * Get the subscription semantics for if a stream should be rejoined after going unavailable.
     *
     * @return the subscription semantics for if a stream should be rejoined after going unavailable.
     * @see CommonContext#REJOIN_PARAM_NAME
     */
    public Boolean rejoin()
    {
        return rejoin;
    }

    /**
     * Initialise a channel for restarting a publication at a given position.
     *
     * @param position      at which the publication should be started.
     * @param initialTermId what which the stream would start.
     * @param termLength    for the stream.
     * @return this for a fluent API.
     */
    public ChannelUriStringBuilder initialPosition(final long position, final int initialTermId, final int termLength)
    {
        if (position < 0 || 0 != (position & (FRAME_ALIGNMENT - 1)))
        {
            throw new IllegalArgumentException("invalid position: " + position);
        }

        final int bitsToShift = LogBufferDescriptor.positionBitsToShift(termLength);

        this.initialTermId = initialTermId;
        this.termId = LogBufferDescriptor.computeTermIdFromPosition(position, bitsToShift, initialTermId);
        this.termOffset = (int)(position & (termLength - 1));
        this.termLength = termLength;

        return this;
    }

    /**
     * Build a channel URI String for the given parameters.
     *
     * @return a channel URI String for the given parameters.
     */
    @SuppressWarnings("MethodLength")
    public String build()
    {
        sb.setLength(0);

        if (null != prefix && !"".equals(prefix))
        {
            sb.append(prefix).append(':');
        }

        sb.append(ChannelUri.AERON_SCHEME).append(':').append(media).append('?');

        if (null != tags)
        {
            sb.append(TAGS_PARAM_NAME).append('=').append(tags).append('|');
        }

        if (null != endpoint)
        {
            sb.append(ENDPOINT_PARAM_NAME).append('=').append(endpoint).append('|');
        }

        if (null != networkInterface)
        {
            sb.append(INTERFACE_PARAM_NAME).append('=').append(networkInterface).append('|');
        }

        if (null != controlEndpoint)
        {
            sb.append(MDC_CONTROL_PARAM_NAME).append('=').append(controlEndpoint).append('|');
        }

        if (null != controlMode)
        {
            sb.append(MDC_CONTROL_MODE_PARAM_NAME).append('=').append(controlMode).append('|');
        }

        if (null != mtu)
        {
            sb.append(MTU_LENGTH_PARAM_NAME).append('=').append(mtu.intValue()).append('|');
        }

        if (null != termLength)
        {
            sb.append(TERM_LENGTH_PARAM_NAME).append('=').append(termLength.intValue()).append('|');
        }

        if (null != initialTermId)
        {
            sb.append(INITIAL_TERM_ID_PARAM_NAME).append('=').append(initialTermId.intValue()).append('|');
        }

        if (null != termId)
        {
            sb.append(TERM_ID_PARAM_NAME).append('=').append(termId.intValue()).append('|');
        }

        if (null != termOffset)
        {
            sb.append(TERM_OFFSET_PARAM_NAME).append('=').append(termOffset.intValue()).append('|');
        }

        if (null != sessionId)
        {
            sb.append(SESSION_ID_PARAM_NAME).append('=').append(prefixTag(isSessionIdTagged, sessionId)).append('|');
        }

        if (null != ttl)
        {
            sb.append(TTL_PARAM_NAME).append('=').append(ttl.intValue()).append('|');
        }

        if (null != reliable)
        {
            sb.append(RELIABLE_STREAM_PARAM_NAME).append('=').append(reliable).append('|');
        }

        if (null != linger)
        {
            sb.append(LINGER_PARAM_NAME).append('=').append(linger.intValue()).append('|');
        }

        if (null != alias)
        {
            sb.append(ALIAS_PARAM_NAME).append('=').append(alias).append('|');
        }

        if (null != cc)
        {
            sb.append(CONGESTION_CONTROL_PARAM_NAME).append('=').append(cc).append('|');
        }

        if (null != sparse)
        {
            sb.append(SPARSE_PARAM_NAME).append('=').append(sparse).append('|');
        }

        if (null != eos)
        {
            sb.append(EOS_PARAM_NAME).append('=').append(eos).append('|');
        }

        if (null != tether)
        {
            sb.append(TETHER_PARAM_NAME).append('=').append(tether).append('|');
        }

        if (null != group)
        {
            sb.append(GROUP_PARAM_NAME).append('=').append(group).append('|');
        }

        if (null != rejoin)
        {
            sb.append(REJOIN_PARAM_NAME).append('=').append(rejoin).append('|');
        }

        final char lastChar = sb.charAt(sb.length() - 1);
        if (lastChar == '|' || lastChar == '?')
        {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    private static String prefixTag(final boolean isTagged, final Integer value)
    {
        return isTagged ? TAG_PREFIX + value : value.toString();
    }
}
