/*
This script is a workaround for cheerpj to extend rotation outside of the
cube area (for desktop browsers).
  n = # cubes on page
  i = interval in milliseconds
  m = max times to run

For a webpage with many cubes, the number can be determined by adding the
following code to it and then pressing the Go button after the page fully
loads (after all cubes are displayed).

<button onclick=ff()>Go</button><br><br>
<script>
function ff() {
  console.log(rtCanv.length);
}
*/ 
var rtCubes, rtInt, rtMax, rtCount, rtCube, rtMouseDown; 
var rtApp = document.getElementsByTagName('object');
var rtCanv = document.getElementsByTagName('canvas');
function rt(n, i, m) {
  rtCubes = n;
  rtInt = i;
  rtMax = m;
  rtCount = 0;
  rt2();
}
function rt2() {
  if (rtCheck() == 1)
    rt3();
  else if (rtCount++ < rtMax)
    setTimeout(rt2, rtInt);
}
function rt3() {
  for (var i=0; i < rtCanv.length; i++) {
    rtCanv[i].n = i;
    rtCanv[i].nextSibling.style.pointerEvents = 'none';
    rtCanv[i].addEventListener('mousedown', mousedown);
    rtCanv[i].addEventListener('mouseup', mouseup);
    rtCanv[i].addEventListener('contextmenu', contextmenu);
  }
  document.addEventListener('mouseup', mouseup);
  document.addEventListener('pointermove', pointermove);
  console.log('rt done');
}
function mousedown(e) {
  rtCube = e.target.n;
  rtMouseDown = true;
  pointerEventsOtherApps('none');
  if (e.button == 2)
    parent.document.addEventListener('contextmenu', contextmenu);
}
function mouseup(e) { 
  if (rtMouseDown) {
    rtMouseDown = false; 
    pointerEventsOtherApps('auto');
  }
  if (e.button == 2)
    setTimeout(removeParentListener, 100);
}
function contextmenu(e) {
  e.preventDefault();
}
function pointermove(e) {
  if (rtMouseDown) {
    var evt = document.createEvent('HTMLEvents');
    evt.initEvent('mousemove', true, false);
    evt.buttons = e.buttons;
    evt.clientX = e.clientX; 
    evt.clientY = e.clientY;
    rtCanv[rtCube].dispatchEvent(evt);
  }
}
function pointerEventsOtherApps(s) {
  for (var i=0; i < rtApp.length; i++) 
    if (i != rtCube) {
      rtApp[i].style.pointerEvents = s;
      rtApp[i].children[2].style.pointerEvents = s;
    }
}
function removeParentListener() {
  parent.document.removeEventListener('contextmenu', contextmenu);
}
function rtCheck() {
  if (rtCanv.length == rtCubes) { 
    var app = rtApp[rtApp.length-1];
    if (typeof app.children[2] != 'undefined') {
      var v = app.children[2];
      for (var i=0; i < 5; i++) {
        if (typeof v.firstChild != 'undefined') {
          v = v.firstChild;
          if (v.nodeName == 'CANVAS')
            break
        }
      }
      if (i == 4 && v.nodeName == 'CANVAS')
        return 1;
    }
  }
  return 0;
}
