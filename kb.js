/*
This script is a workaround for cheerpj to prevent the mobile keyboard from
popping up while rotating a cube.
  n = # input fields on page
  i = interval in milliseconds
  m = max times to run
*/ 
var kbInp, kbInt, kbMax, kbCount;
var kbInput = document.getElementsByTagName('input');
function kb(n, i, m) {
  kbInp = n;
  kbInt = i;
  kbMax = m;
  kbCount = 0;
  kb2();
}
function kb2() {
  for (var i=0; i < kbInput.length; i++)
    if (!kbInput[i].readOnly)
      kbInput[i].readOnly = true;
  if (i < kbInp && kbCount++ < kbMax)
    setTimeout(kb2, kbInt);
  else
    console.log('kb done');
}
