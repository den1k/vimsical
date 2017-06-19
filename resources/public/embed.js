(function() {
  let script = document.currentScript,
      iframe_src = script.getAttribute('data-iframe-src');

  var newFrame = function (iframe_src) {
    let iframe = document.createElement("iframe");
    iframe.src = iframe_src;
    iframe.style = "border:none;width:100%;height:400px;";
    iframe.sandbox = "allow-forms allow-pointer-lock allow-popups allow-same-origin allow-scripts";
    return iframe;
  };

  var insertFrame = function (frame) {
    script.parentNode.replaceChild(frame, script);
  };

  var main = function () {
    insertFrame(newFrame(iframe_src));
  };

  /* Let the host page paint first */
  window.setTimeout(main, 5);

}).call(this);
