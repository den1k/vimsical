(function() {

  // Extract data from the script tag
  let script = document.currentScript,
      script_src = script.getAttribute('src'),
      vims_uid = script.getAttribute('data-vims-uid');

  // Extract the origin from the script src attribute
  var frameOrigin = function (script_src) {
    let parser = document.createElement('a');
    parser.href = script_src;
    return parser.origin;
  };

  // Return a string url for the iFrame src
  var frameSrc = function (script_src, vims_uid) {
    let path = '/embed/?vims-uid=' + vims_uid,
        origin = frameOrigin(script_src),
        url = new URL(path, origin);
    return url.toString();
  };

  // Create a new iFrame DOM element
  var newFrame = function (frame_src) {
    let frame = document.createElement("iframe");
    frame.src = frameSrc(script_src, vims_uid);
    frame.style = "border:none;width:100%;height:400px;";
    frame.sandbox = "allow-forms allow-pointer-lock allow-popups allow-same-origin allow-scripts";
    return frame;
  };

  // Replace the script tag with the fame
  var insertFrame = function (frame) {
    script.parentNode.replaceChild(frame, script);
  };

  var main = function () {
    let frame_src = frameSrc(script_src, vims_uid),
        frame = newFrame(frame_src);
    insertFrame(frame);
  };

  /* Let the host page paint first */
  window.setTimeout(main, 5);

}).call(this);
