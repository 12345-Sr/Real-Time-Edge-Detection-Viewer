
const img = document.getElementById('frame') as HTMLImageElement
const overlay = document.getElementById('overlay') as HTMLDivElement

// Example: embed a base64 image (save an example processed frame as base64)
const sampleBase64 = 'data:image/png;base64,PUT_YOUR_BASE64_HERE'
img.src = sampleBase64
overlay.innerText = 'FPS:  -- (static sample)'
