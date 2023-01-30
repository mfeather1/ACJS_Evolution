/* 
The function "key" in this script is called by cheerpjInit (via the
overrideShortcuts param) to override the browser keyboard shortcuts
(for caesar). The results for the number pad differ between platforms
as shown in the table below (tested in chrome & firefox):

numlock   fedora     windows
-------   --------   --------
  on      all ok     no shift
  off     ctrl-3,9   ctrl-3,9

all ok = all keys work properly with ctrl, alt, shift
ctrl-3,9 = preempted by browser for prev/next tab
no shift = shift disables num lock (not usable as modifier)
*/ 
function key(e) {
  // letters: d,i,k,l,o,p,s
  var c = [68, 73, 75, 76, 79, 80, 83];
  for (var i=0; i < c.length; i++)
    if (e.keyCode === c[i])
      return true;
  // numlock off: 1,3,7,9
  if (e.keyCode >= 33 && e.keyCode <= 36)
    return true;
  // numlock on: 1-9
  if (e.keyCode >= 97 && e.keyCode <= 105)
    return true;
  return false;
}
