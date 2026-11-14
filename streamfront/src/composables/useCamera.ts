
import { ref, onMounted, onUnmounted } from 'vue'

export function useCamera() {
  const videoRef = ref<HTMLVideoElement | null>(null)
  const stream = ref<MediaStream | null>(null)
  const error = ref<string | null>(null)
  const isCameraActive = ref(false)

  // 启动摄像头
  const startCamera = async () => {
    try {
      // 请求用户媒体权限，优先使用前置摄像头
      const mediaStream = await navigator.mediaDevices.getUserMedia({
        video: {
          width: { ideal: 1280 },
          height: { ideal: 720 },
          facingMode: 'user'
        },
        audio: false
      })
      
      stream.value = mediaStream
      if (videoRef.value) {
        videoRef.value.srcObject = mediaStream
        isCameraActive.value = true
        error.value = null
      }
    } catch (err: any) {
      console.error('摄像头访问失败:', err)
      error.value = err.name === 'NotAllowedError' 
        ? '请允许摄像头权限以继续使用此功能' 
        : '无法访问摄像头，请检查设备连接'
      isCameraActive.value = false
    }
  }

  // 停止摄像头
  const stopCamera = () => {
    if (stream.value) {
      stream.value.getTracks().forEach(track => track.stop())
      stream.value = null
      isCameraActive.value = false
      if (videoRef.value) {
        videoRef.value.srcObject = null
      }
    }
  }

  // 拍照并获取 Base64 数据
  const takePhoto = (): string | null => {
    if (!videoRef.value || !stream.value) return null
    
    const canvas = document.createElement('canvas')
    canvas.width = videoRef.value.videoWidth
    canvas.height = videoRef.value.videoHeight
    const ctx = canvas.getContext('2d')
    
    if (ctx) {
      ctx.drawImage(videoRef.value, 0, 0, canvas.width, canvas.height)
      return canvas.toDataURL('image/png')
    }
    return null
  }

  // 组件挂载时自动启动，卸载时自动关闭
  onMounted(startCamera)
  onUnmounted(stopCamera)

  return {
    videoRef,
    error,
    isCameraActive,
    startCamera,
    stopCamera,
    takePhoto
  }
}
