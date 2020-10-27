package org.adelbs.iso8583.protocol;

import java.util.List;

import org.adelbs.iso8583.exception.InvalidPayloadException;
import org.adelbs.iso8583.exception.OutOfBoundsException;
import org.adelbs.iso8583.helper.Iso8583Config;
import org.adelbs.iso8583.util.ISOUtils;

public class ISO8583Length2DelimiterBeginning implements ISO8583Delimiter {

	@Override
	public String getName() {
		return "2 bytes (Length) Delimiter Beginning";
	}

	@Override
	public String getDesc() {
		return "Adds 2 bytes at the beginning of the message. These bytes represent the message size.";
	}

	@Override
	public byte[] preparePayload(ISOMessage isoMessage, Iso8583Config isoConfig) {
		return preparePayload(isoMessage, isoConfig.getStxEtx());
	}
	
	@Override
	public byte[] preparePayload(ISOMessage isoMessage, boolean stxEtx) {
		String sizeHex = Integer.toString(isoMessage.getPayload().length, 16);
		
		if (sizeHex.length() < 2) {
			sizeHex = "0" + sizeHex;	
		}
		
		if (sizeHex.length() < 4) {
			sizeHex = "00" + sizeHex;
		}
		
		byte bt1 = (byte) Integer.parseInt(sizeHex.substring(sizeHex.length() - 4, sizeHex.length() - 2), 16);
		byte bt2 = (byte) Integer.parseInt(sizeHex.substring(sizeHex.length() - 2), 16);

		byte[] data = ISOUtils.mergeArray(new byte[]{bt1, bt2}, isoMessage.getPayload());
		
		if(stxEtx) {
			byte[] btStx = new byte[] {2,70,68,2};
			byte[] btEtx = new byte[] {3,70,68,3};
			data=ISOUtils.mergeArray(btStx,data);
			data=ISOUtils.mergeArray(data, btEtx);
		}
		return data;
	}
	

	@Override
	public boolean isPayloadComplete(List<Byte> bytes, Iso8583Config isoConfig) throws InvalidPayloadException {
		boolean result = false;
		
		if (bytes.size() > 2) {
			try {
				int l;
				l=isoConfig.getStxEtx() ? 4 : 0; //check stxetx
//				Troquei == por >= pq na rede recebe duas mensagens grudadas, testando para ver se ele quebra
				result = bytes.size() >= (getMessageSize(bytes) + 2 + l);
			}
			catch (Exception e) {
				System.out.println("(isPayloadComplete error) hmmmm...");
				throw new InvalidPayloadException(e.getMessage(), e);
			}
		}
		
		return result;
	}

	@Override
	public byte[] clearPayload(byte[] data, Iso8583Config isoConfig) {
		byte[] result = null;
		int l;
		int s;
		try {
			s=2;
			l=data.length;
			
			if(isoConfig.getStxEtx()) {
				l-=4; //remove etx
				//check if payload contains stx
				if(String.format("%02X", ISOUtils.subArray(data, 0, 1)[0]).equals("02") &&
						String.format("%02X", ISOUtils.subArray(data, 3, 4)[0]).equals("02")) {
					s=6; //remove stx
				}
			}
			
			result = ISOUtils.subArray(data, s, l);
		}
		catch (OutOfBoundsException x) {
			x.printStackTrace();
		}
		
		return result;
	}
	
	@Override
	public int getMessageSize(List<Byte> bytes) throws OutOfBoundsException {
		if (bytes.size() > 2) {
			byte[] data = ISOUtils.listToArray(bytes);
			
			String bt1 = String.format("%02X", ISOUtils.subArray(data, 0, 1)[0]);
			String bt2 = String.format("%02X", ISOUtils.subArray(data, 1, 2)[0]);
			
			int messageSize = Integer.parseInt(bt1.concat(bt2), 16);
			
			return messageSize;
		}
		
		return -1;
	}
}
