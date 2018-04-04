window.AudioContext = window.AudioContext || window.webkitAudioContext;
let audioContext = new AudioContext();
let audioRecorder = null;
let analyserContext = null;
let player = document.getElementById('player');
let isRecording = false;
let recIndex = 0;
let recordDuration = 10; // seconds
let recordBtnText = 'RECORD';
let curRecordingData = null;
let intervalId = null;
let timeoutId = null;

$(document).ready(() => {
  let recordDuration = getDuration();
});

function updateFinalQrs (images) {
  $('#finalQrs').empty();

  images.forEach(img => {
    $(`<img class="qrImg" src=${img} />`).appendTo('#finalQrs');
  });
}

function stopRecording() {
  console.log('Stopping recording');
  
  // stop recording
  audioRecorder.stop();

  isRecording = false;

  audioRecorder.exportMp3( doneEncoding );

  clearInterval(intervalId);
  clearTimeout(timeoutId);

  $('#recordBtn').text(recordBtnText);
  $('#recordBtn').attr('recording', 'false');
}

function getDuration() {
  radios = $('.radioBtn');
  for ( let i=0; i < radios.length; i++) {
    if (radios[i].checked) {
      return parseInt(radios[i].id);
    }
  }
  return -1;
}

$('.radioBtn').click(event => {
  recordDuration = getDuration();
});

$('#recordBtn').click(event => {
  
  if (isRecording) {
    stopRecording(intervalId);
    return;
  }

  console.log('Starting recording');
  
  // start recording
  isRecording = true;
  audioRecorder.clear();
  audioRecorder.record();

  let counter = recordDuration;

  $('#recordBtn').text(counter);
  $('#recordBtn').attr('recording', 'true');
  
  intervalId = setInterval(() => {
    counter--;      
    $('#recordBtn').text(counter);
  }, 1000);

  timeoutId = setTimeout(intervalId => {
    if (!audioRecorder) {
      return;
    }
    stopRecording(intervalId);
  }, recordDuration * 1000, intervalId);
})

$('#saveQrBtn').click(event => {
  
  let formData = new FormData();

  // formData.append('description', description);
  formData.append('selectedFile', curRecordingData);

  axios.post('/upload', formData).then((result) => {
    updateFinalQrs(result.data);
  }).catch(err => {
    console.log(err);
  });

});

function updateAnalysers(time) {
  if (!analyserContext) {
      var canvas = document.getElementById("analyser");
      canvasWidth = canvas.width;
      canvasHeight = canvas.height;
      analyserContext = canvas.getContext('2d');
  }

  // analyzer draw code here
  {
      var SPACING = 1;
      var BAR_WIDTH = 1;
      var numBars = Math.round(canvasWidth / SPACING);
      var freqByteData = new Uint8Array(analyserNode.frequencyBinCount);

      analyserNode.getByteFrequencyData(freqByteData); 

      analyserContext.clearRect(0, 0, canvasWidth, canvasHeight);
      analyserContext.fillStyle = 'black';
      // analyserContext.lineCap = 'round';
      var multiplier = analyserNode.frequencyBinCount / numBars;

      // Draw rectangle for each frequency bin.
      for (var i = 0; i < numBars; ++i) {
          var magnitude = 0;
          var offset = Math.floor( i * multiplier );
          // gotta sum/average the block, or we miss narrow-bandwidth spikes
          for (var j = 0; j< multiplier; j++)
              magnitude += freqByteData[offset + j];
          magnitude = magnitude / multiplier;
          var magnitude2 = freqByteData[i * multiplier];
          // analyserContext.fillStyle = "hsl( " + Math.round((i*360)/numBars) + ", 100%, 50%)";
          analyserContext.fillRect(i * SPACING, canvasHeight, BAR_WIDTH, -magnitude);
      }
  }
  
  rafID = window.requestAnimationFrame( updateAnalysers );
}

navigator.mediaDevices.getUserMedia({audio: true, video: false}).then(stream => {
  inputPoint = audioContext.createGain();

  // Create an AudioNode from the stream.
  realAudioInput = audioContext.createMediaStreamSource(stream);
  audioInput = realAudioInput;
  audioInput.connect(inputPoint);

  analyserNode = audioContext.createAnalyser();
  analyserNode.fftSize = 2048;
  inputPoint.connect( analyserNode );

  audioRecorder = new Recorder( inputPoint );

  zeroGain = audioContext.createGain();
  zeroGain.gain.value = 0.0;
  inputPoint.connect( zeroGain );
  zeroGain.connect( audioContext.destination );
  updateAnalysers();
});

function doneEncoding(blob) {
  player.src = window.URL.createObjectURL(blob);

  curRecordingData = blob;
  $('#saveQrBtn').prop('disabled', false);
  
  Recorder.setupDownload( blob, "myRecording" + ((recIndex<10)?"0":"") + recIndex + ".wav" );
  recIndex++;
}
