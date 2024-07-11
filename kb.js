/*
This script is a workaround for cheerpj to prevent the mobile keyboard from
popping up while rotating a cube.

Parameters for kb function:
  n = # of input fields on page (see note below)
  i = interval in milliseconds
  m = max times to run

Note:
The # of input fields on page is the # of cubes + 1, however, schubart's cube
has 6 input fields.

For a webpage with many cubes, the number of input fields can be determined
by adding the following code to it and then pressing the Go button after the
page fully loads (after all cubes are displayed).

<button onclick=ff()>Go</button><br><br>
<script>
function ff() {
  console.log(document.getElementsByTagName('input').length);
}
</script>
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
