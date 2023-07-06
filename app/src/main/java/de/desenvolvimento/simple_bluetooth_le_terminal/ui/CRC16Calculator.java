package de.desenvolvimento.simple_bluetooth_le_terminal.ui;


public class CRC16Calculator {
    public static byte[] calculateCRC16(byte[] data) {
        boolean flagCRCRecepcao = false;
        int CRC = 0x0000ffff;
        int polynomial = 0x0000a001;

        int i, j;
        for (i = 0; i < data.length - 2; i++) {
            CRC ^= (int) (data[i] & 0xffL); //Faz o cast para usar somente o valor inteiro do byte
            for (j = 0; j < 8; j++) {
                if ((CRC & 0x00000001) == 1) {
                    CRC >>= 1;
                    CRC ^= polynomial;
                } else {
                    CRC >>= 1;
                }
            }
        }
        CRC = ((CRC & 0x0000FF00) >> 8) | ((CRC & 0x000000FF) << 8);
//            String result = Integer.toHexString(CRC);
        int CRCILow = (CRC >> 8); //CRC Low
        byte CRCLow = (byte) CRCILow;
        int CRCIHigh = CRC & 0xff;//CRC High
        byte CRCHigh = (byte) CRCIHigh;
        byte retorno[] = {CRCLow,CRCHigh};
        return retorno;
    }
}
