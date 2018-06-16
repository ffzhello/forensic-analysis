package main.java.cn.edu.jslab6.manager;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.IpNumber;

import java.net.Inet4Address;
import java.util.List;

import main.java.cn.edu.jslab6.entity.ActiveTask;
import main.java.cn.edu.jslab6.entity.IpMask;
import main.java.cn.edu.jslab6.utils.IpUtils;

/**
 * @author Andy
 * @date 18-6-16 下午5:51
 * @description
 */
public class PacketMatchManager {

    public static boolean isMatch(PcapHandle pcapHandle, Packet packet, ActiveTask activeTask) throws IllegalRawDataException {
        if(pcapHandle == null || packet == null || activeTask == null) {
            return false;
        }
        // boolean match = true;

        byte[] rawData = packet.getRawData();

        IpV4Packet ipV4Packet = null;
        int v4HeaderLength = Integer.parseInt(Integer.toHexString(rawData[0]).charAt(1) + "") * 4;
        ipV4Packet = IpV4Packet.newPacket(rawData,0, v4HeaderLength);
        IpV4Packet.IpV4Header header = ipV4Packet.getHeader();

        IpNumber protocol = header.getProtocol();
        int pro = protocol.value();//协议号

        if (isProtocolMatch(pro, activeTask.getProtocol()) && isPortMatch(rawData, pro, v4HeaderLength, activeTask) && isIpMatch(header, activeTask)) {
            return true;
        }

        /*//协议
        match = isProtocolMatch(protocol, activeTask.getProtocol());
        if (!match) {
            return false;
        }

        //匹配端口
        match = isPortMatch(rawData, pro, v4HeaderLength, activeTask);
        if (!match) {
            return false;
        }

        match = isIpMatch(header, activeTask);
        if (!match) {
            return false;
        }*/

        return false;
    }

    /*
    判断是否进出双向
     */
    private static boolean isInOutMatch() {

        return true;
    }

    /*
    判断协议
     */
    private static boolean isProtocolMatch(Integer pro, Integer taskPro) {

        if(pro == null || taskPro == null)
            return false;

        if (0xff == taskPro)  //255 匹配所有协议
            return true;

        return (pro == taskPro);
    }
    /*
    判断端口
     */
    private static boolean isPortMatch(byte[] rawData,Integer pro,Integer ipV4HeaderLength, ActiveTask activeTask) throws IllegalRawDataException {

        if (0 == activeTask.getSrcPortDstPort()) {
            if (activeTask.getSrcPort() == -1 || activeTask.getDstPort() == -1) {
                return true;
            }
        }else if (1 == activeTask.getSrcPortDstPort()) {
            if (activeTask.getSrcPort() == -1 && activeTask.getDstPort() == -1) {
                return true;
            }
        }

        int srcPort = 0;
        int dstPort = 0;

        if (pro == null)
            return false;

        if (pro == 6 || pro == 17) {
            if (pro == 6) { //tcp
                TcpPacket tcpPacket = TcpPacket.newPacket(rawData, ipV4HeaderLength, rawData.length-ipV4HeaderLength);
                TcpPacket.TcpHeader tcpHeader = tcpPacket.getHeader();
                srcPort = tcpHeader.getSrcPort().valueAsInt();
                dstPort = tcpHeader.getDstPort().valueAsInt();
            }else if (pro == 17) { //udp
                UdpPacket udpPacket = UdpPacket.newPacket(rawData, ipV4HeaderLength, rawData.length-ipV4HeaderLength);
                UdpPacket.UdpHeader udpHeader = udpPacket.getHeader();
                srcPort = udpHeader.getSrcPort().valueAsInt();
                dstPort = udpHeader.getDstPort().valueAsInt();
            }

            if (0 == activeTask.getSrcPortDstPort()) {
                if (srcPort == activeTask.getSrcPort() || dstPort == activeTask.getDstPort()) {
                    return  true;
                }else {
                    return false;
                }
            }else if (1 == activeTask.getSrcPortDstPort()) {
                if (activeTask.getSrcPort() == -1 && dstPort == activeTask.getDstPort()) {
                    return true;
                }else if (activeTask.getDstPort() == -1 && srcPort == activeTask.getSrcPort()){
                    return  true;
                }else if (srcPort == activeTask.getSrcPort() && dstPort == activeTask.getDstPort()) {
                    return true;
                }
            }
        } else
            return true;

        return false;
    }

    /*
    判断ip地址
    */
    private static boolean isIpMatch(IpV4Packet.IpV4Header header, ActiveTask task) {
        if (header == null || task == null)
            return false;

        //packet srcip dstip
        Inet4Address srcAddr = header.getSrcAddr();
        Inet4Address dstAddr = header.getDstAddr();

        String sAddr = srcAddr.getHostAddress();
        String dAddr = dstAddr.getHostAddress();
        Integer isAddr = IpUtils.ipToInt(sAddr);
        Integer idAddr = IpUtils.ipToInt(dAddr);
        if (isAddr == null || idAddr == null)
            return false;

        //任务待匹配ip
        List<IpMask> ipMasks = task.getIpMaskList();
        if (ipMasks.isEmpty())
            return false;

        for (IpMask ipMask: ipMasks) {
            int taskIp = ipMask.getIp();
            int taskMask = ipMask.getMask();

            if (0 == task.getSrcIPDstIP()) { //或
                if (task.getSrcIP() == 0) {  //不作为源ip
                    if (task.getDstIP() == 0) { //不作为宿ip
                        return true;
                    } else if (task.getDstIP() == 1) { //作为宿ip
                        if ((idAddr & taskMask) == (taskIp & taskMask))
                            return true;
                    }
                } else if (task.getSrcIP() == 1) { //作为源ip
                    if (task.getDstIP() == 0) { //不作为宿ip
                        if ((isAddr & taskMask) == (taskIp & taskMask))
                            return true;
                    } else if (task.getDstIP() == 1) {
                        if (((isAddr & taskMask) == (taskIp & taskMask)) || ((idAddr & taskMask) == (taskIp & taskMask)))
                            return true;
                    }
                }

            }else if (1 == task.getSrcIPDstIP()) { //且
                if (task.getSrcIP() == 0) {  //不作为源ip
                    if (task.getDstIP() == 0) { //不作为宿ip
                        return true;
                    } else if (task.getDstIP() == 1) { //作为宿ip
                        if ((idAddr & taskMask) == (taskIp & taskMask))
                            return true;
                    }
                } else if (task.getSrcIP() == 1) { //作为源ip
                    if (task.getDstIP() == 0) { //不作为宿ip
                        if ((isAddr & taskMask) == (taskIp & taskMask))
                            return true;
                    } else if (task.getDstIP() == 1) {
                        if (((isAddr & taskMask) == (taskIp & taskMask)) && ((idAddr & taskMask) == (taskIp & taskMask)))
                            return true;
                    }
                }
            }
        }
        return false;
    }



    public static void main (String[] args) throws PcapNativeException, NotOpenException, IllegalRawDataException {
        PcapHandle handle = Pcaps.openOffline("D:/1.pcap");

        // ActiveTask task = new ActiveTask();
        // Packet packet = null;
        // packet = handle.getNextPacket();
        //  isMatch(handle,packet,task);

        String ip = "10.153.48.127";
        String mask = "10.153.48.0/10";

        Integer ipp = IpUtils.ipToInt(ip);

        Integer mip = IpUtils.ipToInt(mask);
        Integer ma = IpUtils.getMask(mask);

        System.out.println((ipp & ma)==(mip & ma));

    }
}
