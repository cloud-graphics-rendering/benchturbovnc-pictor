/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * Copyright 2009-2011 Pierre Ossman for Cendio AB
 * Copyright (C) 2011 Brian P. Hinz
 * Copyright (C) 2012, 2015, 2017-2018 D. R. Commander.  All Rights Reserved.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */

package com.turbovnc.rfb;

import java.util.*;
import com.turbovnc.rdr.*;

public class CMsgReaderV3 extends CMsgReader {

  public CMsgReaderV3(CMsgHandler handler_, InStream is_) {
    super(handler_, is_);
    nUpdateRectsLeft = 0;
  }

  public void readServerInit(boolean benchmark) {
    int width = is.readU16();
    int height = is.readU16();
    handler.setDesktopSize(width, height);
    PixelFormat pf = new PixelFormat();
    pf.read(is);
    handler.setPixelFormat(pf);
    String name = is.readString();
    handler.setName(name);
    if (!benchmark &&
        handler.getCurrentCSecurity().getType() == RFB.SECTYPE_TIGHT) {
      int nServerMsg = is.readU16();
      int nClientMsg = is.readU16();
      int nEncodings = is.readU16();
      is.skip(2);
      List<byte[]> serverMsgCaps = new ArrayList<byte[]>();
      for (int i = 0; i < nServerMsg; i++) {
        byte[] cap = new byte[16];
        is.readBytes(cap, 0, 16);
        serverMsgCaps.add(cap);
      }
      List<byte[]> clientMsgCaps = new ArrayList<byte[]>();
      for (int i = 0; i < nClientMsg; i++) {
        byte[] cap = new byte[16];
        is.readBytes(cap, 0, 16);
        clientMsgCaps.add(cap);
      }
      List<byte[]> supportedEncodings = new ArrayList<byte[]>();
      for (int i = 0; i < nEncodings; i++) {
        byte[] cap = new byte[16];
        is.readBytes(cap, 0, 16);
        supportedEncodings.add(cap);
      }
    }
    handler.serverInit();
    System.out.println("CurTime,RTT,ServerHandling,GameHandling,CSI,SP,PSI,AL,ALEnd2FCStart,FC,ASF,TBCP,CP,DecompressionTime,ImageTrans_ntp,Network_Decompression,clientFPS");
  }

  public void readMsg() {
    if (nUpdateRectsLeft == 0) {
      int type = is.readU8();
      switch (type) {
        case RFB.FRAMEBUFFER_UPDATE:
          readFramebufferUpdate();  break;
        case RFB.SET_COLOUR_MAP_ENTRIES:
          readSetColourMapEntries();  break;
        case RFB.BELL:
          readBell();  break;
        case RFB.SERVER_CUT_TEXT:
          readServerCutText();  break;
        case RFB.FENCE:
          readFence();  break;
        case RFB.END_OF_CONTINUOUS_UPDATES:
          readEndOfContinuousUpdates();  break;
        case RFB.GII:
          readGII();  break;
        default:
          vlog.error("Unknown message type " + type);
          throw new ErrorException("Unknown message type " + type);
      }

    } else {
      long time1_decode = System.nanoTime();
      int x = is.readU16();
      int y = is.readU16();
      int w = is.readU16();
      int h = is.readU16();
      int encoding = is.readS32();

      switch (encoding) {
        case RFB.ENCODING_NEW_FB_SIZE:
          handler.setDesktopSize(w, h);
          break;
        case RFB.ENCODING_EXTENDED_DESKTOP_SIZE:
          readExtendedDesktopSize(x, y, w, h);
          break;
        case RFB.ENCODING_DESKTOP_NAME:
          readSetDesktopName(x, y, w, h);
          break;
        case RFB.ENCODING_X_CURSOR:
          readSetXCursor(w, h, new Point(x, y));
          break;
        case RFB.ENCODING_RICH_CURSOR:
          readSetCursor(w, h, new Point(x, y));
          break;
        case RFB.ENCODING_LAST_RECT:
          nUpdateRectsLeft = 1;   // this rectangle is the last one
          readBenchmarkingResults();
          break;
        case RFB.ENCODING_CLIENT_REDIRECT:
          readClientRedirect(x, y, w, h);
          break;
        default:
          readRect(new Rect(x, y, x + w, y + h), encoding);
          break;
      }
      long time2_decode = System.nanoTime();
      long decode_time = time2_decode - time1_decode;
      decode_totalTime += decode_time;

      nUpdateRectsLeft--;
      if (nUpdateRectsLeft == 0){
        if(TotalFrameID != LastTotalFrameID){
            frame_num++;
            LastTotalFrameID = TotalFrameID;
        }
        if(frame_num >= 10){
            long cur_fps_time = System.nanoTime();
            clientFPS = (double)(frame_num * 1e9)/(cur_fps_time - last_fps_time);
            last_fps_time = cur_fps_time;
            frame_num = 0;
        }
	handler.framebufferUpdateEnd();
        if(handle_uTime != 0xdeadbeefL){
            double image_trans_ntp = ((long)System.currentTimeMillis() * 1000 - image_trans_start) * 1e-3;
            double decompression_time = ((double)decode_totalTime)*1e-6;
            double network_decompression = image_trans_ntp - CP;
            System.out.println(java.time.LocalDateTime.now()+","+String.format("%9.02f",RTT)+","+String.format("%9.02f",server_handling)+","+String.format("%9.02f",game_handling)+","+String.format("%9.02f",input_transport)+","+String.format("%9.02f",SP)+","+String.format("%9.02f",PSI)+","+String.format("%9.02f",AL)+","+ String.format("%9.02f",ALEnd2FCStart)+","+String.format("%9.02f",FC)+","+String.format("%9.02f",ASF)+","+String.format("%9.02f",TBCP)+","+String.format("%9.02f",CP)+","+String.format("%9.02f",decompression_time)+","+String.format("%9.02f",image_trans_ntp) +","+String.format("%9.02f", network_decompression)+","+String.format("%9.02f",clientFPS));
        }
      }
    }
  }

  void readFramebufferUpdate() {
    is.skip(1);
    nUpdateRectsLeft = is.readU16();
    is.skip(4);
    image_trans_start = is.readU64();
    TotalFrameID = is.readU64();
    decode_totalTime = 0;
    handler.framebufferUpdateStart();
  }

  void readSetDesktopName(int x, int y, int w, int h) {
    String name = is.readString();

    if (x != 0 || y != 0 || w != 0 || h != 0) {
      vlog.error("Ignoring DesktopName rect with non-zero position/size");
    } else {
      handler.setName(name);
    }
  }
  
  void readBenchmarkingResults() {
    long nsTinput_send = is.readU64();
    long delta = is.readU64();
    long nsTinput_recv = is.readU64();
    long nsTevent_send = is.readU64();//array[3]
    long nsTevent_pickup = is.readU64();
    long nsGameLogicDone = is.readU64();
    long nsTreq_send = is.readU64();
    long nsTreq_pickup = is.readU64();//array[7]
    long nsTupdatebuffer_start = is.readU64();//array[8], before compression.
    long nsTupdate_encoding = is.readU64();
    long nsBeforeCopy = is.readU64(); //array[10]
    long nsAfterCopy  = is.readU64(); //array[11]
    handle_uTime = nsTinput_send & 0xffffffffL;
    if(handle_uTime != 0xdeadbeefL){
        RTT 		= (double)(System.nanoTime() - nsTinput_send)*1e-6;
        input_transport = ((double)delta)*1e-3;
        server_handling = (double)(nsTupdatebuffer_start - nsTinput_recv + nsTupdate_encoding)*1e-6;
        SP 		= (double)(nsTevent_send - nsTinput_recv)*1e-6;//time that events stay on VNCServer
        PSI 		= (double)(nsTevent_pickup - nsTevent_send)*1e-6;//time that events waiting to be deal by app since they are sent.
        AL  		= (double)(nsGameLogicDone - nsTevent_pickup)*1e-6;//App Logic for frame i on CPU
        ALEnd2FCStart 	= (double)(nsBeforeCopy - nsGameLogicDone)*1e-6;//Time for FC(i-1) and AL(i+1)
        FC            	= (double)(nsAfterCopy - nsBeforeCopy)*1e-6;//Time for Frame Copy
        //ASF 		= (double)(nsTupdatebuffer_start - nsTreq_send)*1e-6;
        ASF 		= (double)(nsTreq_pickup - nsTreq_send)*1e-6;
        TBCP 		= (double)(nsTupdatebuffer_start - nsTreq_pickup)*1e-6;
        CP		= ((double)nsTupdate_encoding)*1e-6;
        
        //before_game = (double)(nsTevent_pickup - nsTinput_recv)*1e-6;
        //ReqToCompression = (double)(nsTupdatebuffer_start - nsTreq_send)*1e-6;
        game_handling = (double)(nsTreq_send - nsTevent_pickup)*1e-6;
        if(game_handling < 0){
            System.out.println("negative game handling, [4]:"+ nsTevent_pickup+ "[6]:"+nsTreq_send);
        }
    }
    
  }

  void readExtendedDesktopSize(int x, int y, int w, int h) {
    int screens, i;
    int id, flags;
    int sx, sy, sw, sh;
    ScreenSet layout = new ScreenSet();

    screens = is.readU8();
    is.skip(3);

    for (i = 0; i < screens; i++) {
      id = is.readU32();
      sx = is.readU16();
      sy = is.readU16();
      sw = is.readU16();
      sh = is.readU16();
      flags = is.readU32();

      layout.addScreen(new Screen(id, sx, sy, sw, sh, flags));
    }
    layout.debugPrint("LAYOUT RECEIVED");

    handler.setExtendedDesktopSize(x, y, w, h, layout);
  }

  void readFence() {
    int flags;
    int len;
    byte[] data = new byte[64];

    is.skip(3);

    flags = is.readU32();

    len = is.readU8();
    if (len > data.length) {
      System.err.println("Ignoring fence with too large payload\n");
      is.skip(len);
      return;
    }

    is.readBytes(data, 0, len);

    handler.fence(flags, len, data);
  }

  void readEndOfContinuousUpdates() {
    handler.endOfContinuousUpdates();
  }

  void readGII() {
    int endianAndSubType = is.readU8();
    int endian = endianAndSubType & RFB.GII_BE;
    int subType = endianAndSubType & ~RFB.GII_BE;

    if (endian != RFB.GII_BE) {
      vlog.error("ERROR: don't know how to handle little endian GII messages");
      is.skip(6);
      return;
    }

    int length = is.readU16();
    if (length != 4) {
      vlog.error("ERROR: improperly formatted GII server message");
      is.skip(4);
      return;
    }

    switch (subType) {
      case RFB.GII_VERSION:
        int maximumVersion = is.readU16();
        int minimumVersion = is.readU16();
        if (maximumVersion < 1 || minimumVersion > 1) {
          vlog.error("ERROR: GII version mismatch");
          return;
        }
        if (minimumVersion != maximumVersion)
          vlog.debug("Server supports GII versions " + minimumVersion + " - " +
                     maximumVersion);
        else
          vlog.debug("Server supports GII version " + minimumVersion);
        handler.enableGII();
        break;

      case RFB.GII_DEVICE_CREATE:
        int deviceOrigin = is.readU32();
        if (deviceOrigin == 0) {
          vlog.error("ERROR: Could not create GII device");
          return;
        }
        handler.giiDeviceCreated(deviceOrigin);
        break;
    }
  }

  void readClientRedirect(int x, int y, int w, int h) {
    int port = is.readU16();
    String host = is.readString();
    String x509subject = is.readString();

    if (x != 0 || y != 0 || w != 0 || h != 0) {
      vlog.error("Ignoring ClientRedirect rect with non-zero position/size");
    } else {
      handler.clientRedirect(port, host, x509subject);
    }
  }

  int nUpdateRectsLeft;
  long LastTotalFrameID;
  long TotalFrameID;
  long image_trans_start;
  long last_fps_time;
  long frame_num;
  long handle_uTime;
  long decode_totalTime;
  //long usRect_sendTime;
  double clientFPS;

  double RTT;
  double SP;
  double PSI;
  double AL;
  double ALEnd2FCStart;
  double FC;
  double ASF;
  double TBCP;
  double server_handling;
  //double before_game;
  //double ReqToCompression;
  double game_handling;
  double input_transport;
  double CP;
  static LogWriter vlog = new LogWriter("CMsgReaderV3");
}
