package media.core.rtp.h265;

import media.core.rtp.RtpPacket;
import media.core.rtp.h265.base.FUPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class H265Encoder {

    private static final Logger logger = LoggerFactory.getLogger(H265Encoder.class);

    public H265Encoder() {
        // Nothing
    }

    ////////////////////////////////////////////////////////////////////

    @Deprecated
    public H265Packet packAp (byte[] nalu1, byte[] nalu2) {
        logger.info("Starting to pack AP...");

        if (nalu1 == null || nalu1.length == 0 || nalu2 == null || nalu2.length == 0) {
            logger.warn("Payload is null. Fail to pack AP.");
            return null;
        }
        if (nalu1.length <= RtpPacket.FIXED_HEADER_SIZE || nalu2.length <= RtpPacket.FIXED_HEADER_SIZE) {
            logger.warn("Payload is too short. Fail to pack AP.");
            return null;
        }

        byte[] rtpPayloadNalu1 = new byte[nalu1.length - RtpPacket.FIXED_HEADER_SIZE];
        byte[] rtpHdrNalu1 = new byte[RtpPacket.FIXED_HEADER_SIZE];
        System.arraycopy(nalu1, 0, rtpHdrNalu1, 0, RtpPacket.FIXED_HEADER_SIZE);
        System.arraycopy(nalu1, RtpPacket.FIXED_HEADER_SIZE, rtpPayloadNalu1, 0, nalu1.length - RtpPacket.FIXED_HEADER_SIZE);

        byte[] rtpPayloadNalu2 = new byte[nalu2.length - RtpPacket.FIXED_HEADER_SIZE];
        //byte[] rtpHdrNalu2 = new byte[RtpPacket.FIXED_HEADER_SIZE];
        //System.arraycopy(nalu2, 0, rtpHdrNalu2, 0, RtpPacket.FIXED_HEADER_SIZE);
        System.arraycopy(nalu2, RtpPacket.FIXED_HEADER_SIZE, rtpPayloadNalu2, 0, nalu2.length - RtpPacket.FIXED_HEADER_SIZE);

        logger.debug("\tNALU1: {} (len={})", rtpPayloadNalu1, rtpPayloadNalu1.length);
        logger.debug("\tNALU2: {} (len={})", rtpPayloadNalu2, rtpPayloadNalu2.length);

        byte[] apData = new byte[RtpPacket.FIXED_HEADER_SIZE + nalu1.length + nalu2.length + 8]; // 8 bytes:
        System.arraycopy(rtpHdrNalu1, 0, apData, 0, RtpPacket.FIXED_HEADER_SIZE);
        int index = RtpPacket.FIXED_HEADER_SIZE;
        apData[index] = 48 << 1;
        apData[1 + index] = 1;

        apData[2 + index] = (byte) (rtpPayloadNalu1.length >> 8);
        apData[3 + index] = (byte) (rtpPayloadNalu1.length);

        apData[rtpPayloadNalu1.length + 4 + index] = (byte) (rtpPayloadNalu2.length >> 8);
        apData[rtpPayloadNalu1.length + 5 + index] = (byte) (rtpPayloadNalu2.length & 0xff);

        System.arraycopy(rtpPayloadNalu1, 0, apData, 4 + index, rtpPayloadNalu1.length);
        System.arraycopy(rtpPayloadNalu2, 0, apData, 6 + index + rtpPayloadNalu1.length, rtpPayloadNalu2.length);

        logger.debug("\tAP: {}", Arrays.toString(apData));
        logger.info("Success to pack AP.");

        return new H265Packet(apData, RtpPacket.RTP_PACKET_MAX_SIZE, true);
    }

    public H265Packet packApByList (List<H265Packet> naluList) {
        logger.info("Starting to pack AP...");

        if (naluList == null || naluList.isEmpty()) {
            logger.warn("AP List is null or empty. Fail to pack AP.");
            return null;
        }

        int totalDataLen = 0;
        List<byte[]> apList = new ArrayList<>();
        for (H265Packet h265Packet : naluList) {
            byte[] nalu = h265Packet.getRawData();
            if (nalu == null || nalu.length == 0) {
                logger.warn("Payload is null. Fail to pack AP.");
                return null;
            }
            if (nalu.length <= RtpPacket.FIXED_HEADER_SIZE) {
                logger.warn("Payload is too short. Fail to pack AP.");
                return null;
            }

            byte[] rtpPayloadNalu = new byte[nalu.length - RtpPacket.FIXED_HEADER_SIZE];
            byte[] rtpHdrNalu = new byte[RtpPacket.FIXED_HEADER_SIZE];
            System.arraycopy(nalu, 0, rtpHdrNalu, 0, RtpPacket.FIXED_HEADER_SIZE);
            System.arraycopy(nalu, RtpPacket.FIXED_HEADER_SIZE, rtpPayloadNalu, 0, nalu.length - RtpPacket.FIXED_HEADER_SIZE);

            byte[] apData;
            int index;

            if (totalDataLen == 0) {
                apData = new byte[RtpPacket.FIXED_HEADER_SIZE + rtpPayloadNalu.length +
                        H265Packet.RTP_HEVC_PAYLOAD_HEADER_SIZE + H265Packet.RTP_HEVC_AP_NALU_LENGTH_FIELD_SIZE];
                System.arraycopy(rtpHdrNalu, 0, apData, 0, RtpPacket.FIXED_HEADER_SIZE);
                index = RtpPacket.FIXED_HEADER_SIZE;

                apData[index] = 48 << 1;
                apData[1 + index] = 1;
                apData[2 + index] = (byte) (rtpPayloadNalu.length >> 8);
                apData[3 + index] = (byte) (rtpPayloadNalu.length);
                System.arraycopy(rtpPayloadNalu, 0, apData,
                        H265Packet.RTP_HEVC_PAYLOAD_HEADER_SIZE + H265Packet.RTP_HEVC_AP_NALU_LENGTH_FIELD_SIZE + index,
                        rtpPayloadNalu.length);
            } else {
                index = 0;
                apData = new byte[rtpPayloadNalu.length + H265Packet.RTP_HEVC_AP_NALU_LENGTH_FIELD_SIZE];
                apData[0] = (byte) (rtpPayloadNalu.length >> 8);
                apData[1] = (byte) (rtpPayloadNalu.length);
                System.arraycopy(rtpPayloadNalu, 0, apData, H265Packet.RTP_HEVC_AP_NALU_LENGTH_FIELD_SIZE + index, rtpPayloadNalu.length);
            }

            apList.add(apData);

            logger.debug("\tNALU: {} (len={})", apData, apData.length);
            totalDataLen += apData.length;
        }

        if (totalDataLen > RtpPacket.RTP_PACKET_MAX_SIZE) {
            logger.warn("Total payload length is more than RTP Packet max size. Fail to pack AP. (payloadLen={}, rtpMaxSize={})",
                    totalDataLen, RtpPacket.RTP_PACKET_MAX_SIZE);
            return null;
        }

        int curLen = 0;
        byte[] totalData = new byte[totalDataLen];
        for (byte[] ap : apList) {
            System.arraycopy(ap, 0, totalData, curLen, ap.length);
            curLen += ap.length;
        }

        logger.debug("\tAP: {}", totalData);
        logger.info("Success to pack AP.");

        return new H265Packet(totalData, RtpPacket.RTP_PACKET_MAX_SIZE, true);
    }

    ////////////////////////////////////////////////////////////////////

    public H265Packet packFu (H265Packet h265Packet, FUPosition fuPosition) {
        logger.info("Starting to pack FU...");

        byte[] rawPayload = h265Packet.getRawData();
        if (rawPayload == null || rawPayload.length == 0) {
            logger.warn("Payload is null. Fail to pack FU.");
            return null;
        }

        int packetLength = h265Packet.getLength();
        int payloadLength = packetLength - RtpPacket.FIXED_HEADER_SIZE;

        byte[] buffer = new byte[packetLength + 3];
        byte[] rtpHdrNalu = new byte[RtpPacket.FIXED_HEADER_SIZE];
        byte[] rtpPayloadNalu = new byte[payloadLength];
        System.arraycopy(rawPayload, 0, rtpHdrNalu, 0, RtpPacket.FIXED_HEADER_SIZE);
        logger.debug("rtpHdrNalu: {}", rtpHdrNalu);
        System.arraycopy(rawPayload, RtpPacket.FIXED_HEADER_SIZE, rtpPayloadNalu, 0, payloadLength);
        logger.debug("rtpPayloadNalu: {}", rtpPayloadNalu);

        byte[] header = new byte[3];
        header[0] = 49 << 1;
        header[1] = 1;
        if (fuPosition == FUPosition.START) {
            header[2] = (byte) h265Packet.getType();
            header[2] += 0b10000000; // S = 1
        } else if (fuPosition == FUPosition.MIDDLE) {
            header[2] = (byte) (h265Packet.getType() & 0b00111111);
            // S = 0, E = 0
        } else {
            header[2] = (byte) h265Packet.getType();
            header[2] += 0b01000000; // E = 1
        }

        System.arraycopy(rtpHdrNalu, 0, buffer, 0, RtpPacket.FIXED_HEADER_SIZE);
        System.arraycopy(header, 0, buffer, RtpPacket.FIXED_HEADER_SIZE, 3);

        //rtpPayloadNalu[0] = 49;
        System.arraycopy(rtpPayloadNalu, 0, buffer, RtpPacket.FIXED_HEADER_SIZE + 3, payloadLength);
        logger.debug("Packed FU: {}, len: {}", buffer, buffer.length);

        logger.info("Success to pack FU.");
        return new H265Packet(buffer, RtpPacket.RTP_PACKET_MAX_SIZE, true);
    }

}