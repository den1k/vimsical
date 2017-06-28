// CatchAll Errors
onerror = function () {
  return true;
};

// Stop and Resume `requestAnimationFrame`
var __rafTimerIds = [];
var __rafCbsSet = new Set();
var __rafCbs = [];

var __raf =
  requestAnimationFrame ||
  mozRequestAnimationFrame ||
  webkitRequestAnimationFrame;
var __cancelRaf = cancelAnimationFrame || mozCancelAnimationFrame;

function __trackingRaf(cb) {
  if (!__rafCbsSet.has(cb)) {
    __rafCbs.push(cb)
    __rafCbsSet.add(cb)
  }
  var timerID = __raf(cb);
  __rafTimerIds.push(timerID);
  return timerID;
}

requestAnimationFrame = mozRequestAnimationFrame = webkitRequestAnimationFrame = __trackingRaf;

function __cancelRafs() {
  for (let id of __rafTimerIds) {
    __cancelRaf(id);
  }
  __rafTimerIds = [];
}

function __resumeRafs() {
  var rafCbs = __rafCbs;
  __rafCbs = [];
  __rafCbsSet = new Set();
  for (let cb of rafCbs) {
    __trackingRaf(cb);
  }
}

// Stop and Resume CSS Animations
var __styleEl = document.createElement("style");
__styleEl.textContent =
  "*, *::before, *::after { animation-play-state: paused !important; }";

function __stopAnimations() {
  document.head.prepend(__styleEl);
}

function __resumeAnimations() {
  __styleEl.remove();
}

function __freeze() {
  __cancelRafs()
  __stopAnimations()
}

function __defreeze() {
  __resumeRafs()
  __resumeAnimations()
}