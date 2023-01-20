
import java.awt.*;
import java.awt.event.*;
import java.applet.*;

// I compressed all wanted features (event handlers, threads, controls,...) into one class...
// yes, it can be done in Java, but I have been thinking of it intensively :-)
public final class Cube extends Applet implements Runnable, MouseListener, MouseMotionListener {
  // many things are inspired by Karl, but code is written from scratch...
  Thread animThread = null; // thread for performing the cube animation
  // double buffered animation
  Graphics offGr = null;
  Image offIm = null;
  // cube window size (applet window is resizable!)
  int scrW, scrH;
  // last position of mouse (for dragging the cube)
  int lastX, lastY;
  // background colors
  Color BGCol, butBGCol;
  // cube colors
  Color[] cols = new Color[8];
  // all facelets
  byte[] flets = new byte[54];

  // normal vectors U,D,F,B,L,R
  static final double[] sideVec = {0,-1,0, 0,1,0, 0,0,-1, 0,0,1, -1,0,0, 1,0,0};

  // vertex co-ordinates UFL,UFR,UBR,UBL,DFL,DFR,DBR,DBL
  static final double[] cornVec = {-1,-1,-1, +1,-1,-1, +1,-1,+1, -1,-1,+1, -1,+1,-1, +1,+1,-1, +1,+1,+1, -1,+1,+1};
  // subcubes' vertex co-ordinates
  double[] TCornVec = new double[24], BCornVec = new double[24];

  // sides' vertexes
  static final byte[] sideCorns = {0,1,2,3, 4,7,6,5, 0,4,5,1, 2,6,7,3, 0,3,7,4, 1,5,6,2};

  // sides' adjacent sides
  static final byte[] adjSides = {2,5,3,4, 4,3,5,2, 4,1,5,0, 5,1,4,0, 0,3,1,2, 2,1,3,0};
  // current twisted side
  byte twSide = -1;
  // dimensions in number of facelets (mincol, maxcol, minrow, maxrow)
  static final byte[] sideBlocks = {0,3,0,3, 0,3,0,3, 0,3,0,3, 0,3,0,3, 0,3,0,3, 0,3,0,3};
  // subcubes' dimensions
  byte[] TBlocks = new byte[24], BBlocks = new byte[24];
  // all possible subcubes' dimensions
  static final byte[] TBl = {0,3,0,1, 0,1,0,3, 0,3,2,3, 2,3,0,3, 0,0,0,0, 0,3,0,3};
  static final byte[] BBl = {0,3,0,2, 0,2,0,3, 0,3,1,3, 1,3,0,3, 0,0,0,0, 0,3,0,3};
  // indexes to TBl[] and BBl[] for particular twSide
  static final byte[] TBlD = {5,4,1,1,0,1, 4,5,3,3,2,3, 0,1,5,4,1,0, 2,3,4,5,3,2, 1,0,0,2,5,4, 3,2,2,0,4,5};
  static final byte[] BBlD = {4,5,3,3,2,3, 5,4,1,1,0,1, 2,3,4,5,3,2, 0,1,5,4,1,0, 3,2,2,0,4,5, 1,0,0,2,5,4};
  // top facelet cycle
  static final byte[] cycleOrder = {0, 1, 2, 5, 8, 7, 6, 3};
  // side facelet cycle
  static final byte[] cycleS = {1,0, 3,2, -1,8, -3,6};
  // indexes to cycleS[] for particular twSide
  static final byte[] cycleSides = {3,3,3,0, 2,1,1,1, 3,3,0,0, 2,1,1,2, 3,2,0,0, 2,2,0,1};
  // directions of facelet cycling for all sides
  static final byte[] colDir = {+1, +1, -1, -1, -1, -1};

  // dragging regions etc...
  byte drRegs;
  double[] drCorns = new double[96], drDirs = new double[24];
  static final byte[] drCoef = {0,0,1,1, 0,3,3,0, 3,3,2,2, 3,0,0,3, 0,0,1,1};
  static final byte[] drDcoef = {1,0,-1,0, 0,1,0,-1};
  static final byte[] twDirs = {+1,+1,+1,+1, +1,+1,+1,+1, +1,-1,+1,-1, +1,-1,+1,-1, -1,+1,-1,+1, +1,-1,+1,-1};
  byte[] drSides = new byte[12]; // which sides belongs to drCorns
  double cddx, cddy; // current drDirs

  // initial observer co-ordinate axes (view)
  double[] eE = {0.5446, -0.4670, -0.6966};
  double[] eX = {0.7884, 0.0016, 0.6152}; // (sideways)
  double[] eY = new double[3]; // (vertical)
  double[] TeE = new double[3], TeX = new double[3], TeY = new double[3];
  // projected vertex co-ordinates (on screen)
  double[] projCoords = new double[16];
  // angle of rotation of the twSide
  double phi, phi0 = 0;

  // current states of the program (self-describing)
  boolean natState = true, canDrag = false, interrupted = false, inverse = false;
  boolean twisting = false, spinning = false, animating = false;

  // move sequence data
  byte[] move = new byte[200];
  int moveMax = -1, movePos = 0, moveDir = 1;
  boolean moveOne, moveAnm;

  // buttons' states
  final int butH = 15;
  boolean butsChanged = true, butPushed = false;
  int butPressed = -1;

  // do some initializing
  public void init() {
    cols[0] = new Color(255, 255, 255); // white
    cols[1] = new Color(255, 255,   0); // yellow
    cols[2] = new Color(255,  48,   0); // orange
    cols[3] = new Color(160,   0,   0); // red
    cols[4] = new Color(  0, 112,   0); // green
    cols[5] = new Color(  0,  32, 208); // blue
    cols[6] = new Color(176, 176, 176); // light gray
    cols[7] = new Color( 80,  80,  80); // dark gray
    normVec(vecProd(eY, eE, eX)); // fix y axis of observer co-ordinate system
    getBGCol(); // find out the background color
    getFLCol(); // setup cube facelets
    getMove(); // setup move sequence
    // make this class be event handler
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  // we don't need start()...
  public void stop() {
    stopAnim();
  }

  // start particular amination; preemptive multitasking required
  public synchronized void startAnim(int mode) {
    stopAnim();
    animThread = new Thread(this, "Cube Animator"); // make new thread from this class
    moveDir = 1;
    moveOne = false;
    moveAnm = true;
    switch (mode) {
     case 0: // play forward
      if (movePos >= moveMax)
        movePos = -1;
      break;
     case 1: // play backward
      if (movePos < 0)
        movePos = moveMax;
      moveDir = -1;
      break;
     case 2: // step forward
      if (movePos >= moveMax)
        movePos = -1;
      moveDir = 1;
      moveOne = true;
      break;
     case 3: // step backward
      if (movePos < 0)
        movePos = moveMax;
      moveDir = -1;
      moveOne = true;
      break;
     case 4: // fast forward
      if (movePos >= moveMax)
        movePos = -1;
      moveDir = 1;
      moveAnm = false;
      break;
     case 5: // rewind
      if (movePos < 0 && moveMax >= 0)
        movePos = moveMax;
      else {
        movePos = -1;
        getFLCol(); // reset cube facelets
      }
      moveAnm = false;
      moveDir = -1;
      break;
    }
    animThread.start(); // run animation assynchronously
    // and return for handling next user inputs
  }

  // stops assynchronous animation (if any)
  public void stopAnim() {
    synchronized (this) { // prevent finishing/starting thread after the following test
      if (animThread == null || !animThread.isAlive())
        return;
      // critical point
      animThread.interrupt();
    }
    while (animThread.isAlive()) {
      try {
        Thread.sleep(50); // don't eat too much processor's cycles
      }
      catch (InterruptedException e) {}
    }
  }

  // Runnable interface's method

  public void run() {
    animating = true;
    interrupted = false;
    if (moveDir > 0)
      movePos += moveDir;
    for (; movePos >= 0 && movePos <= moveMax; movePos += moveDir) {
      if (moveDir < 0)
        spin((byte)(move[movePos] / 3), 3 - move[movePos] % 3, moveAnm);
      else
        spin((byte)(move[movePos] / 3), 1 + move[movePos] % 3, moveAnm);
      synchronized (this) {
        if (moveOne || interrupted || Thread.interrupted()) {
          if (moveDir < 0)
            movePos += moveDir;
          break;
        }
      }
    }
    if (movePos > moveMax)
      movePos = moveMax;
    if (movePos < -1)
      movePos = -1;
    animating = false;
    butsChanged = true;
    repaint();
  }

  // animate the turn of one side of the cube
  public void spin(byte layer, int num, boolean anim) {
    twisting = false;
    natState = true;
    spinning = true;
    phi0 = 0;
    if (colDir[layer] > 0)
      num = (4 - num) % 4;
    if (anim) {
      double phit = 3.14159 / 2; // target for phi (default pi/2)
      double phis = 1; // sign of phiq
      int turnTime = 400; // milliseconds to be used for one turn
      long sTime, lTime;

      if (num == 2) {
        phit = 3.14159;
        turnTime = 600;
      }
      if (num == 3)
        phis = -1;
      twisting = true;
      cutUpCube(twSide = layer); // start twisting
      natState = false;
      lTime = sTime = System.currentTimeMillis();
      double d = phis * phit / turnTime;
      for (phi = 0; phi * phis < phit; phi = d * (lTime - sTime)) {
        repaint();
        try {
          Thread.sleep(25); // many programmers forget this (hi Lars :-))
        }
        catch (InterruptedException e) {
          interrupted = true;
          break;
        }
        lTime = System.currentTimeMillis();                                                
      }                                                                                          
    }
    synchronized (this) { // don't colide with the paint method
      phi = 0;
      twisting = false;
      natState = true;
      if (colDir[layer] < 0)
        twistLayer(layer, 4 - num);
      else
        twistLayer(layer, num);
      spinning = false;
    }
    repaint();
  }                                                                                              


  // convert hexadecimal RGB parameters to colors
  public void getBGCol() {
    final String h = "0123456789abcdef";
    byte[] hex = new byte[6];
    String s = getParameter("bgcolor"); // background color for main area
    if (s != null && s.length() == 6) {
      for (int i = 0; i < 6; i++)
        for (byte j = 0; j < 16; j++)
          if (Character.toLowerCase(s.charAt(i)) == h.charAt(j))
            hex[i] = j;
      BGCol = new Color(hex[0] * 16 + hex[1], hex[2] * 16 + hex[3], hex[4] * 16 + hex[5]);
    }
    else
      BGCol = Color.gray; // Default
    s = getParameter("butbgcolor"); // background for buttons
    if (s != null && s.length() == 6) {
      for (int i = 0; i < 6; i++)
        for (byte j = 0; j < 16; j++)
          if (Character.toLowerCase(s.charAt(i)) == h.charAt(j))
            hex[i] = j;
      butBGCol = new Color(hex[0] * 16 + hex[1], hex[2] * 16 + hex[3], hex[4] * 16 + hex[5]);
    }
    else
      butBGCol = BGCol;
  }

  // convert facelets parameter string to 54 facelets of the cube
  public void getFLCol() {
    final String f = "wyorgbld";
    String s = getParameter("facelets");
    if (s != null && s.length() == 54) {
      for (int i = 0; i < 54; i++) {
        flets[i] = 7;
        for (byte j = 0; j < 8; j++)
          if (Character.toLowerCase(s.charAt(i)) == f.charAt(j))
            flets[i] = j;
      }
    }
    else
      for (int i = 0; i < 54; i++)
        flets[i] = (byte)(i / 9);
  }

  // convert symbolic input move sequence to array of bytes
  public void getMove() {
    final String f = "udfblr";
    String s = getParameter("move");
    moveMax = -1;
    movePos = -1;
    if (s == null)
      return;
    for (int i = 0; i < s.length(); i++) {
      for (byte j = 0; j < 6; j++) {
        if (Character.toLowerCase(s.charAt(i)) == f.charAt(j)) {
          i++;
          if (i < s.length() && s.charAt(i) == '\'')
            move[++moveMax] = (byte)(j * 3 + 2);
          else if (i < s.length() && s.charAt(i) == '2')
            move[++moveMax] = (byte)(j * 3 + 1);
          else {
            move[++moveMax] = (byte)(j * 3);
            i--;
          }
          break;
        }
      }
    }
  }


  // produce large and small sub-cube for twisting
  public void cutUpCube(int layer) {
    boolean flag;
    double[] t1 = new double[3], t2 = new double[3];

    // duplicate main co-ordinate data
    for (int i = 0; i < 24; i++) {
      TCornVec[i] = cornVec[i];
      BCornVec[i] = cornVec[i];
    }
    // start manipulating and build new parts
    copyVec(t1, 0, sideVec, 3 * layer);
    copyVec(t2, t1);
    // fix new co-ordinates; some need to be altered
    scalMult(t1, 4.0 / 3.0);
    scalMult(t2, 2.0 / 3.0);
    for (int i = 0; i < 8; i++) {
      flag = false;
      for (int j = 0; j < 4; j++)
        if (i == sideCorns[layer * 4 + j])
          flag = true;
      if (flag)
        subVec(BCornVec, i * 3, t2, 0);
      else
        addVec(TCornVec, i * 3, t1, 0);
    }
    // the sub-cubes need information about which colored fields belong to them.
    for (int i = 0; i < 6; i++) { // for all sides
      for (int j = 0; j < 4; j++) { // set dimensions
        TBlocks[i * 4 + j] = TBl[TBlD[layer * 6 + i] * 4 + j];
        BBlocks[i * 4 + j] = BBl[BBlD[layer * 6 + i] * 4 + j];
      }
    }
  }


  // cycle facelets of the particular side
  public void twistLayer(int layer, int num) {
    byte[] twbuf = new byte[12];

    // rotate top facelets
    for (int i = 0, k = num * 2; i < 8; i++)
      twbuf[k++ % 8] = flets[layer * 9 + cycleOrder[i]];
    for (int i = 0; i < 8; i++)
      flets[layer * 9 + cycleOrder[i]] = twbuf[i];
    // rotate side facelets
    for (int i = 0, k = num * 3; i < 4; i++) {
      int n = adjSides[layer * 4 + i], c = cycleSides[layer * 4 + i];
      int c1 = cycleS[c * 2], c2 = cycleS[c * 2 + 1];
      for (int j = 0; j < 3; j++)
        twbuf[k++ % 12] = flets[n * 9 + c1 * j + c2];
    }
    for (int i = 0, k = 0; i < 4; i++) {
      int n = adjSides[layer * 4 + i], c = cycleSides[layer * 4 + i];
      int c1 = cycleS[c * 2], c2 = cycleS[c * 2 + 1];
      for (int j = 0; j < 3; j++)
        flets[n * 9 + c1 * j + c2] = twbuf[k++];
    }
  }


  // main paint method
  public synchronized void paint(Graphics g) {
    Dimension dim = getSize();
    final byte[] rotcos = {1,0,0, 0,0,0, 0,0,1,  1,0,0, 0,1,0, 0,0,0,  0,0,0, 0,1,0, 0,0,1};
    final byte[] rotsin = {0,0,1, 0,0,0,-1,0,0,  0,1,0,-1,0,0, 0,0,0,  0,0,0, 0,0,1,0,-1,0};
    final byte[] rotvec = {0,0,0, 0,1,0, 0,0,0,  0,0,0, 0,0,0, 0,0,1,  1,0,0, 0,0,0, 0,0,0};
    final byte[] rotsig = {1,-1,1,-1,1,-1};

    // create offscreen buffer for double buffering
    if (offIm == null || dim.width != scrW || dim.height - butH != scrH) {
      offIm = createImage(scrW = dim.width, scrH = dim.height);
      offGr = offIm.getGraphics();
      scrH -= butH;
      butsChanged = true;
    }
    drRegs = 0;
    offGr.setColor(BGCol); // clear drawing buffer
    offGr.fillRect(0, 0, scrW, scrH);
    if (natState)
      fixBlock(eE, eX, eY, cornVec, sideBlocks, 0); // draw cube and fill drag regions
    else { // in twisted state? then compute top observer
      double cosphi = Math.cos(phi + phi0);
      double sinphi = rotsig[twSide] * Math.sin(phi + phi0);
      int t = twSide / 2 * 9;
      for (int i = 0; i < 3; i++) {
        TeE[i] = 0;
        TeX[i] = 0;
        for (int j = 0; j < 3; j++) {
          int k = t + i * 3 + j;
          TeE[i] += eE[j] * (rotvec[k] + rotcos[k] * cosphi + rotsin[k] * sinphi);
          TeX[i] += eX[j] * (rotvec[k] + rotcos[k] * cosphi + rotsin[k] * sinphi);
        }
      }
      vecProd(TeY, TeE, TeX);
      if (scalProd(eE, 0, sideVec, twSide * 3) < 0) { // top facing away? draw it first
        fixBlock(TeE, TeX, TeY, TCornVec, TBlocks, 2);
        fixBlock( eE,  eX,  eY, BCornVec, BBlocks, 1);
      }
      else {
        fixBlock( eE,  eX,  eY, BCornVec, BBlocks, 1);
        fixBlock(TeE, TeX, TeY, TCornVec, TBlocks, 2);
      }
    }
    // draw outlined numbers (not very fast way... :-))
    String s = "" + (movePos + 1) + "/" + (moveMax + 1);
    int w = offGr.getFontMetrics().stringWidth(s);
    offGr.setColor(Color.black);
    offGr.drawString(s, scrW - w - 1, scrH - 2);
    offGr.drawString(s, scrW - w - 3, scrH - 4);
    offGr.drawString(s, scrW - w - 3, scrH - 2);
    offGr.drawString(s, scrW - w - 1, scrH - 4);
    offGr.drawString(s, scrW - w - 3, scrH - 3);
    offGr.drawString(s, scrW - w - 1, scrH - 3);
    offGr.drawString(s, scrW - w - 2, scrH - 2);
    offGr.drawString(s, scrW - w - 2, scrH - 4);
    offGr.setColor(Color.white);
    offGr.drawString(s, scrW - w - 2, scrH - 3);
    if (butsChanged) { // omit unneccessary butons' redrawing
      drawButs(offGr);
      butsChanged = false;
    }
    g.drawImage(offIm, 0, 0, this); // at last show all
  }

  // update method should not clear the background
  public void update(Graphics g) {
    paint(g);
  }


  // draw cube or sub-cube and compute draging regions
  public void fixBlock(double[] beE, double[] beX, double[] beY, double[] bcornVec, byte[] bblocks, int mode) {
    int[] rectX = new int[4], rectY = new int[4]; // drawn rectangles in fixBlock()
    double sx = 0, sy = 0, sdxh = 0, sdyh = 0, sdxv = 0, sdyv = 0;
    // borders of the facelets
    final double[] flBrd = {0.1, 0.1, 0.9, 0.1, 0.9, 0.9, 0.1, 0.9};

    for (int i = 0; i < 8; i++) { // project 3D co-ordinates into 2D screen ones
      if (inverse)
        projCoords[i * 2]   = scrW / 2 - scrW / 3.53 * scalProd(bcornVec, i * 3, beX, 0);
      else
        projCoords[i * 2]   = scrW / 2 + scrW / 3.53 * scalProd(bcornVec, i * 3, beX, 0);
      projCoords[i * 2 + 1] = scrH / 2 - scrH / 3.53 * scalProd(bcornVec, i * 3, beY, 0);
    }

    for (int i = 0; i < 6; i++) { // process all sides
      if (scalProd(beE, 0, sideVec, 3 * i) > 0) { // face towards us? then draw it
        offGr.setColor(Color.black);
        for (int j = 0; j < 4; j++) { // find corner co-ordinates
          rectX[j] = (int)projCoords[2 * sideCorns[i * 4 + j]];
          rectY[j] = (int)projCoords[2 * sideCorns[i * 4 + j] + 1];
        }
        offGr.fillPolygon(rectX, rectY, 4); // first draw black
        int sideW = bblocks[i * 4 + 1] - bblocks[i * 4];
        int sideH = bblocks[i * 4 + 3] - bblocks[i * 4 + 2];
        if (sideW > 0 && sideH > 0) { // sideX == 0 ? then this side is black
          sx = projCoords[2 * sideCorns[i * 4]];
          sy = projCoords[2 * sideCorns[i * 4] + 1];
          sdxh = (projCoords[2 * sideCorns[i * 4 + 1]] - sx) / sideW;
          sdxv = (projCoords[2 * sideCorns[i * 4 + 3]] - sx) / sideH;
          sdyh = (projCoords[2 * sideCorns[i * 4 + 1] + 1] - sy) / sideW;
          sdyv = (projCoords[2 * sideCorns[i * 4 + 3] + 1] - sy) / sideH;
          // then draw colored facelets
          for (int n = 0, p = bblocks[i * 4 + 2]; n < sideH; n++, p++) {
            for (int o = 0, q = bblocks[i * 4]; o < sideW; o++, q++) {
              for (int k = 0; k < 4; k++) {
                rectX[k] = (int)(sx + (o + flBrd[k * 2]) * sdxh + (n + flBrd[k * 2 + 1]) * sdxv);
                rectY[k] = (int)(sy + (o + flBrd[k * 2]) * sdyh + (n + flBrd[k * 2 + 1]) * sdyv);
              }
              offGr.setColor(cols[flets[i * 9 + p * 3 + q]]);
              offGr.fillPolygon(rectX, rectY, 4);
            }
          }
        } // sideW > 0
        if (animating) // don't need twisting while animating
          continue;
        switch (mode) { // determine allowed drag regions and directions
         case 0: // just the normal cube
          for (int j = 0; j < 4; j++) { // 4 regions per side
            for (int k = 0; k < 4; k++) { // 4 points per region
              drCorns[drRegs * 8 + k]     = sx + drCoef[j * 4 + k + 4] * sdxh + drCoef[j * 4 + k] * sdxv;
              drCorns[drRegs * 8 + k + 4] = sy + drCoef[j * 4 + k + 4] * sdyh + drCoef[j * 4 + k] * sdyv;
            }
            drDirs[drRegs * 2]     = (sdxh * drDcoef[j] + sdxv * drDcoef[j + 4]) * twDirs[i * 4 + j];
            drDirs[drRegs * 2 + 1] = (sdyh * drDcoef[j] + sdyv * drDcoef[j + 4]) * twDirs[i * 4 + j];
            drSides[drRegs] = adjSides[i * 4 + j];
            drRegs++;
          }
          break;
         case 1: // the large sub-cube
          // could not drag part of the larger sub-cube
          break;
         case 2: // the small sub-cube (twistable part)
          if (i != twSide && sideW > 0 && sideH > 0) { // only 3x1 sides
            for (int k = 0; k < 4; k++) {
              drCorns[drRegs * 8 + k]     = projCoords[2 * sideCorns[i * 4 + k]];
              drCorns[drRegs * 8 + k + 4] = projCoords[2 * sideCorns[i * 4 + k] + 1];
            }
            if (sideW == 3) // determine positive drag direction (sideH == 1)
              if (bblocks[i * 4 + 2] == 0) { // minRow == 0, maxRow == 1
                drDirs[drRegs * 2]     = sdxh * twDirs[i * 4];
                drDirs[drRegs * 2 + 1] = sdyh * twDirs[i * 4];
              }
              else { // minRow == 2, maxRow == 3
                drDirs[drRegs * 2]     = -sdxh * twDirs[i * 4 + 2];
                drDirs[drRegs * 2 + 1] = -sdyh * twDirs[i * 4 + 2];
              }
            else // sideW == 1, sideH == 3
              if (bblocks[i * 4] == 0) { // minCol == 0, maxCol == 1
                drDirs[drRegs * 2]     = -sdxv * twDirs[i * 4 + 3];
                drDirs[drRegs * 2 + 1] = -sdyv * twDirs[i * 4 + 3];
              }
              else { // minCol == 2, maxCol == 3
                drDirs[drRegs * 2]     = sdxv * twDirs[i * 4 + 1];
                drDirs[drRegs * 2 + 1] = sdyv * twDirs[i * 4 + 1];
              }
            drSides[drRegs] = twSide;
            drRegs++;
          }
          break;
        }
      }
    }
  }

  // redraw control pannel
  void drawButs(Graphics g) {
    int x = 0, y = scrH;
    int w = scrW, h = butH, w1 = w / 7, h2 = h / 2, w2 = w1 / 2;

    if (!butPushed && !animating) // no button should be deceased
      butPressed = -1;
    for (int i = 0, x1 = x; i < 7; i++, x1 += w1) {
      if (x1 + w1 > x + w - 7) // correction for the last button (not very nice)
        w2 = (w1 = x + w - x1) / 2;
      g.setColor(butBGCol);
      g.fill3DRect(x1, y, w1, h, i != butPressed);
      drawBut(g, i, x1 + w2, y + h2);
    }
  }

  // draw particular button with number i
  void drawBut(Graphics g, int i, int x, int y) {
    g.setColor(Color.white);
    switch (i) {
     case 0: // rw
      drawArrow(g, x + 4, y, false);
      drawArrow(g, x, y, false);
      break;
     case 1: // rev step
      g.fillRect(x + 3, y - 2, 1, 5);
      g.setColor(Color.black);
      g.drawRect(x + 2, y - 3, 2, 6);
      drawArrow(g, x, y, false);
      break;
     case 2: // rev play
      drawArrow(g, x + 2, y, false);
      break;
     case 3: // stop
      g.fillRect(x - 2, y - 2, 5, 5);
      g.setColor(Color.black);
      g.drawRect(x - 3, y - 3, 6, 6);
      break;
     case 4: // play
      drawArrow(g, x - 2, y, true);
      break;
     case 5: // step
      g.fillRect(x - 3, y - 2, 1, 5);
      g.setColor(Color.black);
      g.drawRect(x - 4, y - 3, 2, 6);
      drawArrow(g, x, y, true);
      break;
     case 6: // ff
      drawArrow(g, x - 4, y, true);
      drawArrow(g, x, y, true);
      break;
    }
  }

  // nice bordered arrows :-))
  void drawArrow(Graphics g, int x, int y, boolean right) {
    g.setColor(Color.black);
    g.drawLine(x, y - 3, x, y + 3);
    x += right ? 1 : -1;
    for (int i = 0; i >= -3 && i <= 3; i += right ? 1 : -1) {
      int j = right ? 3 - i : 3 + i;
      g.drawLine(x + i, y + j, x + i, y - j);
    }
    g.setColor(Color.white);
    for (int i = 0; i >= -1 && i <= 1; i += right ? 1 : -1) {
      int j = right ? 1 - i : 1 + i;
      g.drawLine(x + i, y + j, x + i, y - j);
    }
  }

  // figure out which button was selected and perform the specific action
  void selectButs(int mx, int my) {
    final int[] butAnim = {5, 3, 1, -1, 0, 2, 4};
    int x = 0, y = scrH;
    int w = scrW, h = butH, w1 = w / 7, h2 = h / 2, w2 = w1 / 2;

    butsChanged = true;
    for (int i = 0, x1 = x; i < 7; i++, x1 += w1) {
      if (x1 + w1 > x + w - 7)
        w1 = x + w - x1;
      if (mx >= x1 && mx < x1 + w1 && my >= y && my < y + h) {
        butPressed = i;
        break;
      }
    }
    if (butPressed >= 0 && butPressed <= 6) {
      if (butPressed == 3 && !animating) // special feature (I like this idea :-))
        inverse = !inverse;
      stopAnim();
      butPushed = true;
      if (butPressed != 3)
        startAnim(butAnim[butPressed]);
    }
  }

  // MouseMotionListener interface's methods

  public void mouseDragged(MouseEvent e) {
    boolean flag = false;
    double x1, x2, y1, y2;
    int x = e.getX(), y = e.getY();
    int dx = x - lastX, dy = y - lastY;

    if (!twisting && canDrag && !animating) { // we don't twist but we can
      canDrag = false;
      for (int i = 0; i < drRegs; i++) { // check if inside a drag region
        double d1 = drCorns[i * 8];
        double d2 = drCorns[i * 8 + 4];
        x1 = drCorns[i * 8 + 1] - d1;
        y1 = drCorns[i * 8 + 3] - d1;
        x2 = drCorns[i * 8 + 5] - d2;
        y2 = drCorns[i * 8 + 7] - d2;
        double a = ( y2*(lastX-d1) - y1*(lastY-d2)) / (x1*y2 - y1*x2);
        double b = (-x2*(lastX-d1) + x1*(lastY-d2)) / (x1*y2 - y1*x2);
        if (a > 0 && a < 1 && b > 0 && b < 1) { // we are in
          cddx = drDirs[i * 2];
          cddy = drDirs[i * 2 + 1];
          double d = cddx*dx + cddy*dy;
          d = d*d / ((cddx*cddx + cddy*cddy) * (dx*dx + dy*dy));
          if (d > 0.6) {
            flag = true;
            twSide = drSides[i];
            break;
          }
        }
      }
      if (flag) { // twisting?
        if (natState) { // the cube still hasn't been split up
          cutUpCube(twSide);
          natState = false;
        }
        twisting = true;
        phi = 0.03 * (cddx*dx + cddy*dy) / Math.sqrt(cddx*cddx + cddy*cddy);
        repaint();
        return;
      }
    }
    canDrag = false;
    if (!twisting || animating) { // Normal rotation
      if (butPushed) // button in active state
        return;
      double[] t = new double[3];
      if (inverse)
        normVec(addVec(eE, scalMult(copyVec(t, eX), dx * 0.016)));
      else
        normVec(addVec(eE, scalMult(copyVec(t, eX), dx * -0.016)));
      normVec(vecProd(eX, eY, eE));
      normVec(addVec(eE, scalMult(copyVec(t, eY), dy * 0.016)));
      normVec(vecProd(eY, eE, eX));
      lastX = x;
      lastY = y;
      repaint();
    }
    else { // Twist, compute twisting angle phi
      phi = 0.03 * (cddx*dx + cddy*dy) / Math.sqrt(cddx*cddx + cddy*cddy);
      repaint();
    }
  }

  public void mouseMoved(MouseEvent e) {} // no use for this...

  // MouseListener interface's methods

  public void mousePressed(MouseEvent e) {
    lastX = e.getX();
    lastY = e.getY();
    if (lastY >= scrH) // pressed on the control pannel
      selectButs(lastX, lastY);
    else
      canDrag = true;
    repaint();
  }

  public void mouseReleased(MouseEvent e) {
    if (butPushed) {
      butPushed = false;
      butsChanged = true;
      repaint();
      return;
    }
    if (twisting && !spinning) {
      twisting = false;
      phi0 += phi;
      phi = 0;
      double ang = phi0;
      while (ang < 0)
        ang += 32.0 * 3.14159; // 32 x pi
      int num = (int)(ang * 8.0 / 3.14159); // 8/pi .. pi/2 = quarter -> 4
      if (num % 4 == 0 || num % 4 == 3) { // close enough to a corner?
        num = (num + 1) / 4 % 4;
        if (colDir[twSide] < 0)
          num = (4 - num) % 4;
        phi0 = 0;
        natState = true; // return the cube to its natural state
        twistLayer(twSide, num); // and rotate the facelets
      }
      repaint();
    }
  }

  // no use for theese...
  public void mouseClicked(MouseEvent e) {}

  public void mouseEntered(MouseEvent e) {}

  public void mouseExited(MouseEvent e) {}


// various vector manipulation functions

  static double scalProd(double[] v1, int i1, double[] v2, int i2) {
    return v1[i1] * v2[i2] + v1[i1+1] * v2[i2+1] + v1[i1+2] * v2[i2+2];
  }

  static double vecLen(double[] v, int i) {
    return Math.sqrt(v[i] * v[i] + v[i+1] * v[i+1] + v[i+2] * v[i+2]);
  }

  static double vecLen(double[] v) {
    return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
  }

  static double[] normVec(double[] v) {
    double t = vecLen(v);
    v[0] /= t;
    v[1] /= t;
    v[2] /= t;
    return v;
  }

  static double[] scalMult(double[] v, double a) {
    v[0] *= a;
    v[1] *= a;
    v[2] *= a;
    return v;
  }

  static double[] addVec(double[] v1, int i1, double[] v2, int i2) {
    v1[i1]   += v2[i2];
    v1[i1+1] += v2[i2+1];
    v1[i1+2] += v2[i2+2];
    return v1;
  }

  static double[] addVec(double[] v1, double[] v2) {
    v1[0] += v2[0];
    v1[1] += v2[1];
    v1[2] += v2[2];
    return v1;
  }

  static double[] subVec(double[] v1, int i1, double[] v2, int i2) {
    v1[i1]   -= v2[i2];
    v1[i1+1] -= v2[i2+1];
    v1[i1+2] -= v2[i2+2];
    return v1;
  }

  static double[] subVec(double[] v1, double[] v2) {
    v1[0] -= v2[0];
    v1[1] -= v2[1];
    v1[2] -= v2[2];
    return v1;
  }

  static double[] copyVec(double[] v1, int i1, double[] v2, int i2) {
    v1[i1]   = v2[i2];
    v1[i1+1] = v2[i2+1];
    v1[i1+2] = v2[i2+2];
    return v1;
  }

  static double[] copyVec(double[] v1, double[] v2) {
    v1[0] = v2[0];
    v1[1] = v2[1];
    v1[2] = v2[2];
    return v1;
  }

  static double[] vecProd(double[] v1, double[] v2, double[] v3) {
    v1[0] = v2[1] * v3[2] - v2[2] * v3[1];
    v1[1] = v2[2] * v3[0] - v2[0] * v3[2];
    v1[2] = v2[0] * v3[1] - v2[1] * v3[0];
    return v1;
  }

}

