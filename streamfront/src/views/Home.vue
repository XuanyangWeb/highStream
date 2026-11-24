<script setup lang="ts">
import { ref } from 'vue';

// 1. 任务数据模型
// const tasks = ref([
//     { id: '1', title: '体验 Tailwind v4 新特性', isCompleted: false },
//     { id: '2', title: '掌握 Composition API', isCompleted: true }
// ]);
const manualMessage = ref("")
const statusColorClass = {
    'connected': 'text-green-600',
    'connecting': 'text-yellow-600',
    'error': 'text-red-600',
    'disconnected': 'text-slate-500'
}
const frameCount = ref(0)
const connectionStatus = ref('connected')
const isRecording = ref<boolean>(false)
//摄像头
const videoRef = ref<HTMLVideoElement | null>(null)
const stream = ref<MediaStream | null>(null)
const isActive = ref(false)

const streamActive = ref(false)
const streamActiveStatus = ref(false)
const error = ref<string | null>(null)

//const inputText = ref('')
const receivedMessages = ref([
    // 初始可以放一些测试数据，结构需与模板中使用的字段一致
    {
        time: '10:00:01',
        data: 'WebSocket 连接成功'
    },
    {
        time: '10:05:23',
        data: '收到服务器推送的消息内容'
    }
]);
const wsUrl = ref('sdf')


//发送弹幕
const sendManualMessage = () => {
    // 在这里编写点击后要执行的逻辑，比如你可以做二次校验、组装消息
    if (connectionStatus.value !== 'connecting') {
        console.warn('当前WebSocket未连接，无法发送')
        return
    }
    const msg = manualMessage.value?.trim();
    if (!msg) {
        console.log('消息不能')
    }
}
//断连
const disconnect = () => {

}
//链接
const connect = () => {

}

//开始推流
const toggleRecording = () => {
    isRecording.value = !isRecording.value
    console.log("当前isRecording的状态：" + isRecording.value)
    if (isRecording.value) {
        const videoElement = videoRef;
        const whipURl = "localhost:8848/room//start"
        pushWebRTCWithWHIP(whipURl);
    } else {
        disconnect()
    }

}

//开始推送流量的方法
async function pushWebRTCWithWHIP(whipUrl: string) {
   
    console.log("jin  pushWebRTCWithWHIP")
    // 1. 获取本地摄像头和麦克风流
    const stream = await navigator.mediaDevices.getUserMedia({
        video: true,
        audio: true,
    });

    console.log(stream.active)
    console.log("aaaaa")
    // 2. 创建 PeerConnection（仅推流设为 sendonly）
    const pc = new RTCPeerConnection({
        iceServers: [{ urls: "stun:://google.com" }],
    });


    //遍历MediaStream中所有的音频轨道，逐个调用addTrack方法绑定到链接
    stream.getTracks().forEach((track) => {
        pc.addTransceiver(track, { direction: "sendonly" });
    });

    console.log("111绑定完成")
    // 3. 创建 Offer 并等待 ICE 收集完成（省去逐条发 Candidate 的代码）
    const offer = await pc.createOffer();
    //把SDP绑定到本地连接
    await pc.setLocalDescription(offer);

    console.log("111SDP绑定到本地")
    await new Promise<void>((resolve) => {
        if (pc.iceGatheringState === "complete") return resolve();
        const checkState = () => {
            if (pc.iceGatheringState === "complete") {
                pc.removeEventListener("icegatheringstatechange", checkState);
                resolve();
            }
        };
        pc.addEventListener("icegatheringstatechange", checkState);
    });

    // 4. 通过一次 HTTP POST 把 SDP 发给后端，换取 Answer
    const res = await fetch(whipUrl, {
        method: "POST",
        headers: { "Content-Type": "application/sdp" },
        body: pc.localDescription!.sdp,
    });

    if (!res.ok) throw new Error(`推流失败: ${res.status}`);

    //后端的应答报文
    const answerSdp = await res.text();
    //设置远程描述
    //ICE候选交换和媒体传输通道才会真正打通
    await pc.setRemoteDescription({
        type: "answer",
        sdp: answerSdp
    });

    // 返回停止推流函数
    return () => {
        stream.getTracks().forEach((t) => t.stop());
        pc.close();
    };
}

const toggleCamera = () => {

    streamActive.value = !streamActive.value
    //关闭摄像头 默认false -已开启
    if (streamActive.value) {
        //页面中央
        streamActiveStatus.value = !streamActiveStatus.value
        console.log("开始打开摄像头")
        startCamera()

    } else {
        streamActiveStatus.value = !streamActiveStatus.value
        stopCamera()

    }
}

//查看是否支持摄像头
const checkSupport = () => {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        error.value = '当前浏览器不支持摄像头访问';
        return false;
    }
    return true;
};

//打开摄像头 
const startCamera = async (constraints: MediaStreamConstraints = { video: true }) => {

    console.log("开始检验")
    if (!checkSupport()) return


    const mediaStream = await navigator.mediaDevices.getUserMedia(constraints);
    console.log("检验成功1")
    stream.value = mediaStream;
    isActive.value = true;
    error.value = null;

    if (videoRef.value) {
        videoRef.value.srcObject = mediaStream;
        videoRef.value.play().catch((e) => {
            console.error('视频播放失败', e);
        })
    }
    console.log("检验成功2")

}

//关闭摄像头
const stopCamera = () => {
    if (stream.value) {
        //
        stream.value.getTracks().forEach((element) => {
            element.stop();
        });
        stream.value = null;
    }

    if (videoRef.value) {
        videoRef.value.srcObject = null;
    }
    isActive.value = false;

}
const clearLogs = () => {

}

</script>


<template>
    <div class="min-h-screen flex flex-col">

        <!--<header class="bg-linear-to-br from-blue-600 to-indigo-700 p-8 text-white">
            <h1 class="text-3xl font-black tracking-tight">HighStream</h1>
        </header>
    -->
        <header class="bg-linear-to-br from-blue-600 to-indigo-700 p-8 text-white">
            <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
                <div class="flex items-center gap-3">
                    <h1 class="text-3xl font-black tracking-tight">HighStream</h1>
                </div>
                <div class="flex items-center gap-4">
                    <span class="text-sm text-slate-500 hidden sm:inline-block">WebSocket 实时通讯演示</span>
                    <a href="#" class="text-slate-400 hover:text-indigo-600 transition-colors">
                        <i class="fa-solid fa-gear"></i>
                    </a>
                </div>
            </div>
        </header>
        <main class="grow container mx-auto px-4 py-8 max-w-6xl">

            <!-- 状态提示栏 -->
            <transition name="fade">
                <div v-if="true" :class="{
                    'bg-green-50 border-green-200 text-green-800': false,
                    'bg-yellow-50 border-yellow-200 text-yellow-800': true,
                    'bg-red-50 border-red-200 text-red-800': false
                }" class="mb-6 p-4 rounded-lg border flex items-center justify-between shadow-sm">
                    <div class="flex items-center gap-3">
                        <i :class="{
                            'fa-circle-check': false,
                            'fa-circle-notch fa-spin': true,
                            'fa-circle-exclamation': false
                        }" class="fa-solid"></i>
                        <span class="font-medium"> statusMessage </span>
                    </div>
                    <button v-if="true" @click="disconnect" class="text-xs font-semibold underline hover:no-underline">
                        断开连接
                    </button>
                </div>
            </transition>
            <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">

                <!-- 左侧：控制与视频区域 -->
                <div class="lg:col-span-2 space-y-6">

                    <!-- 视频预览卡片 -->
                    <div class="bg-white rounded-2xl shadow-lg overflow-hidden border border-slate-100">
                        <div class="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50/50">
                            <h2 class="font-semibold text-slate-700 flex items-center gap-2">
                                <i class="fa-solid fa-video text-indigo-500"></i>
                                本地摄像头预览
                            </h2>
                            <div class="flex gap-2">
                                <span v-if=isRecording
                                    class="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-red-100 text-red-600 text-xs font-bold animate-pulse">
                                    <span class="w-2 h-2 bg-red-600 rounded-full"></span>
                                    REC
                                </span>
                                <span v-else
                                    class="px-2.5 py-1 rounded-full bg-slate-100 text-slate-500 text-xs font-bold">
                                    STOPPED
                                </span>
                            </div>
                        </div>

                        <div class="relative aspect-video bg-black group">
                            <video ref="videoRef" autoplay playsinline muted
                                class="absolute inset-0 w-full h-full object-cover"></video>

                            <!-- 覆盖层：未开启摄像头时显示 -->
                            <div v-if="!streamActiveStatus"
                                class="absolute inset-0 flex flex-col items-center justify-center text-slate-400 bg-slate-900/80 backdrop-blur-sm">
                                <i class="fa-solid fa-camera-slash text-4xl mb-3 opacity-50"></i>
                                <p class="text-sm">摄像头未开启</p>
                            </div>

                            <!-- 覆盖层：连接错误时显示 -->
                            <div v-if="connectionStatus === 'error'"
                                class="absolute inset-0 flex flex-col items-center justify-center text-white bg-red-900/80 backdrop-blur-sm z-10">
                                <i class="fa-solid fa-triangle-exclamation text-4xl mb-3"></i>
                                <p class="text-sm font-medium">WebSocket 连接失败</p>
                                <button @click="connect"
                                    class="mt-4 px-4 py-2 bg-white text-red-900 rounded-lg text-sm font-bold hover:bg-red-50 transition">
                                    重试连接
                                </button>
                            </div>
                        </div>

                        <!-- 控制按钮栏 -->
                        <div
                            class="p-4 bg-white flex flex-wrap gap-3 justify-center sm:justify-start border-t border-slate-100">
                            <button @click="toggleCamera" :disabled="connectionStatus === 'connecting'"
                                :class="streamActive ? 'bg-slate-100 text-slate-700 hover:bg-slate-200' : 'bg-indigo-600 text-white hover:bg-indigo-700 shadow-md hover:shadow-lg'"
                                class="px-5 py-2.5 rounded-lg font-medium transition-all duration-200 flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
                                <i :class="streamActive ? 'fa-video-slash' : 'fa-video'"></i>
                                {{ streamActive ? '关闭摄像头' : '开启摄像头' }}
                            </button>

                            <!--streamActive = true - 已打开摄像头
                                streamActive = false 未打开摄像头
                                打开摄像头 并且 不connected
                            -->
                            <button @click="toggleRecording" :disabled="!streamActive"
                                :class="isRecording ? 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-50' : ' bg-red-500 text-white hover:bg-red-600 shadow-red-200'"
                                class="px-5 py-2.5 rounded-lg font-medium transition-all duration-200 flex items-center gap-2 shadow-sm hover:shadow-md disabled:opacity-50 disabled:cursor-not-allowed">
                                <i :class="isRecording ? 'fa-stop' : 'fa-microphone'"></i>
                                {{ isRecording ? '停止推流' : '开始推流' }}
                            </button>

                            <div class="grow"></div>

                            <!-- 设置模拟 -->
                            <div
                                class="flex items-center gap-2 text-sm text-slate-500 bg-slate-50 px-3 py-2 rounded-lg border border-slate-100">
                                <i class="fa-solid fa-server"></i>
                                <span>WS: {{ wsUrl }}</span>
                            </div>
                        </div>
                    </div>

                    <!-- 技术参数展示 -->
                    <div class="bg-white rounded-xl shadow-sm border border-slate-100 p-5">
                        <h3 class="text-sm font-bold text-slate-400 uppercase tracking-wider mb-4">会话信息</h3>
                        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                            <div class="space-y-1">
                                <p class="text-xs text-slate-500">连接状态</p>
                                <p class="font-mono text-sm font-semibold" :class="statusColorClass">{{
                                    connectionStatus.toUpperCase() }}</p>
                            </div>
                            <div class="space-y-1">
                                <p class="text-xs text-slate-500">媒体轨道</p>
                                <p class="font-mono text-sm font-semibold text-slate-700">{{ streamActive ? 'ACTIVE' :
                                    'INACTIVE' }}</p>
                            </div>
                            <div class="space-y-1">
                                <p class="text-xs text-slate-500">发送帧数</p>
                                <p class="font-mono text-sm font-semibold text-slate-700">{{ frameCount }}</p>
                            </div>
                            <div class="space-y-1">
                                <p class="text-xs text-slate-500">接收消息</p>
                                <p class="font-mono text-sm font-semibold text-slate-700">{{ receivedMessages.length }}
                                </p>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 右侧：消息日志区域 -->
                <div class="lg:col-span-1">
                    <div class="bg-white rounded-2xl shadow-lg border border-slate-100 h-150 flex flex-col">
                        <div class="p-4 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
                            <h2 class="font-semibold text-slate-700 flex items-center gap-2">
                                <i class="fa-solid fa-list-ul text-indigo-500"></i>
                                实时消息日志
                            </h2>
                            <button @click="clearLogs"
                                class="text-xs text-slate-400 hover:text-red-500 transition-colors">
                                <i class="fa-solid fa-trash-can mr-1"></i>清空
                            </button>
                        </div>

                        <div class="grow overflow-y-auto p-4 space-y-3 bg-slate-50/30" ref="logContainer">
                            <div v-if="receivedMessages.length === 0"
                                class="h-full flex flex-col items-center justify-center text-slate-400 opacity-60">
                                <i class="fa-regular fa-comments text-3xl mb-2"></i>
                                <p class="text-sm">暂无消息记录</p>
                            </div>

                            <transition-group name="list" tag="div" class="space-y-3">
                                <div v-for="(msg, index) in receivedMessages" :key="index"
                                    class="bg-white p-3 rounded-lg border border-slate-100 shadow-sm text-sm animate-in fade-in slide-in-from-bottom-2 duration-300">
                                    <div class="flex justify-between items-start mb-1">
                                        <span
                                            class="text-xs font-bold text-indigo-600 bg-indigo-50 px-1.5 py-0.5 rounded">SERVER</span>
                                        <span class="text-xs text-slate-400 font-mono">{{ msg.time }}</span>
                                    </div>
                                    <p class="text-slate-700 wrapper-break-word">{{ msg.data }}</p>
                                </div>
                            </transition-group>
                        </div>

                        <!-- 底部输入模拟（可选，用于测试双向通信） -->
                        <div class="p-3 border-t border-slate-100 bg-white">
                            <div class="flex gap-2">
                                <input type="text" v-model="manualMessage" placeholder="发送消息..."
                                    class="flex-1 bg-slate-50 border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all">
                                <button @click="sendManualMessage"
                                    :disabled="connectionStatus !== 'connected' && !manualMessage.trim()"
                                    class="bg-indigo-600 text-white px-3 py-2 rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
                                    <i class="fa-solid fa-paper-plane">Go</i>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </main>

    </div>
</template>

<style scoped>
/* 列表过渡动画 */
.list-enter-active,
.list-leave-active {
    transition: all 0.4s cubic-bezier(0.18, 0.89, 0.32, 1.28);
}

.list-enter-from,
.list-leave-to {
    opacity: 0;
    transform: translateX(30px);
}
</style>