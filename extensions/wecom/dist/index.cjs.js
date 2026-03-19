'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var pluginSdk = require('openclaw/plugin-sdk');
var os$1 = require('os');
var path$1 = require('path');
var aibotNodeSdk = require('@wecom/aibot-node-sdk');
var fs = require('node:fs/promises');
var path = require('node:path');
var os = require('node:os');
var node_url = require('node:url');
var url = require('url');
var fileType = require('file-type');

var _documentCurrentScript = typeof document !== 'undefined' ? document.currentScript : null;
function _interopNamespaceDefault(e) {
    var n = Object.create(null);
    if (e) {
        Object.keys(e).forEach(function (k) {
            if (k !== 'default') {
                var d = Object.getOwnPropertyDescriptor(e, k);
                Object.defineProperty(n, k, d.get ? d : {
                    enumerable: true,
                    get: function () { return e[k]; }
                });
            }
        });
    }
    n.default = e;
    return Object.freeze(n);
}

var os__namespace$1 = /*#__PURE__*/_interopNamespaceDefault(os$1);
var path__namespace$1 = /*#__PURE__*/_interopNamespaceDefault(path$1);
var fs__namespace = /*#__PURE__*/_interopNamespaceDefault(fs);
var path__namespace = /*#__PURE__*/_interopNamespaceDefault(path);
var os__namespace = /*#__PURE__*/_interopNamespaceDefault(os);

let runtime = null;
function setWeComRuntime(r) {
    runtime = r;
}
function getWeComRuntime() {
    if (!runtime) {
        throw new Error("WeCom runtime not initialized - plugin not registered");
    }
    return runtime;
}

/**
 * openclaw plugin-sdk 高版本方法兼容层
 *
 * 部分方法（如 loadOutboundMediaFromUrl、detectMime、getDefaultMediaLocalRoots）
 * 仅在较新版本的 openclaw plugin-sdk 中才导出。
 *
 * 本模块在加载时一次性探测 SDK 导出，存在则直接 re-export SDK 版本，
 * 不存在则导出 fallback 实现。其他模块统一从本文件导入，无需关心底层兼容细节。
 */
const _sdkReady = import('openclaw/plugin-sdk')
    .then((sdk) => {
    const exports$1 = {};
    if (typeof sdk.loadOutboundMediaFromUrl === "function") {
        exports$1.loadOutboundMediaFromUrl = sdk.loadOutboundMediaFromUrl;
    }
    if (typeof sdk.detectMime === "function") {
        exports$1.detectMime = sdk.detectMime;
    }
    if (typeof sdk.getDefaultMediaLocalRoots === "function") {
        exports$1.getDefaultMediaLocalRoots = sdk.getDefaultMediaLocalRoots;
    }
    return exports$1;
})
    .catch(() => {
    // openclaw/plugin-sdk 不可用或版本过低，全部使用 fallback
    return {};
});
// ============================================================================
// detectMime —— 检测 MIME 类型
// ============================================================================
const MIME_BY_EXT = {
    ".heic": "image/heic",
    ".heif": "image/heif",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".png": "image/png",
    ".webp": "image/webp",
    ".gif": "image/gif",
    ".ogg": "audio/ogg",
    ".mp3": "audio/mpeg",
    ".m4a": "audio/x-m4a",
    ".mp4": "video/mp4",
    ".mov": "video/quicktime",
    ".pdf": "application/pdf",
    ".json": "application/json",
    ".zip": "application/zip",
    ".gz": "application/gzip",
    ".tar": "application/x-tar",
    ".7z": "application/x-7z-compressed",
    ".rar": "application/vnd.rar",
    ".doc": "application/msword",
    ".xls": "application/vnd.ms-excel",
    ".ppt": "application/vnd.ms-powerpoint",
    ".docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    ".xlsx": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    ".pptx": "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    ".csv": "text/csv",
    ".txt": "text/plain",
    ".md": "text/markdown",
    ".amr": "audio/amr",
    ".aac": "audio/aac",
    ".wav": "audio/wav",
    ".webm": "video/webm",
    ".avi": "video/x-msvideo",
    ".bmp": "image/bmp",
    ".svg": "image/svg+xml",
};
/** 通过 buffer 魔术字节嗅探 MIME 类型（动态导入 file-type，不强依赖） */
async function sniffMimeFromBuffer(buffer) {
    try {
        const { fileTypeFromBuffer } = await import('file-type');
        const type = await fileTypeFromBuffer(buffer);
        return type?.mime ?? undefined;
    }
    catch {
        return undefined;
    }
}
/** fallback 版 detectMime，参考 weclaw/src/media/mime.ts */
async function detectMimeFallback(opts) {
    const ext = opts.filePath ? path__namespace.extname(opts.filePath).toLowerCase() : undefined;
    const extMime = ext ? MIME_BY_EXT[ext] : undefined;
    const sniffed = opts.buffer ? await sniffMimeFromBuffer(opts.buffer) : undefined;
    const isGeneric = (m) => !m || m === "application/octet-stream" || m === "application/zip";
    if (sniffed && (!isGeneric(sniffed) || !extMime)) {
        return sniffed;
    }
    if (extMime) {
        return extMime;
    }
    const headerMime = opts.headerMime?.split(";")?.[0]?.trim().toLowerCase();
    if (headerMime && !isGeneric(headerMime)) {
        return headerMime;
    }
    if (sniffed) {
        return sniffed;
    }
    if (headerMime) {
        return headerMime;
    }
    return undefined;
}
/**
 * 检测 MIME 类型（兼容入口）
 *
 * 支持两种调用签名以兼容不同使用场景：
 * - detectMime(buffer)           → 旧式调用
 * - detectMime({ buffer, headerMime, filePath }) → 完整参数
 *
 * 优先使用 SDK 版本，不可用时使用 fallback。
 */
async function detectMime(bufferOrOpts) {
    const sdk = await _sdkReady;
    const opts = Buffer.isBuffer(bufferOrOpts)
        ? { buffer: bufferOrOpts }
        : bufferOrOpts;
    if (sdk.detectMime) {
        try {
            return await sdk.detectMime(opts);
        }
        catch {
            // SDK detectMime 异常，降级到 fallback
        }
    }
    return detectMimeFallback(opts);
}
// ============================================================================
// loadOutboundMediaFromUrl —— 从 URL/路径加载媒体文件
// ============================================================================
/** 安全的本地文件路径校验，参考 weclaw/src/web/media.ts */
async function assertLocalMediaAllowed(mediaPath, localRoots) {
    if (!localRoots || localRoots.length === 0) {
        throw new Error(`Local media path is not under an allowed directory: ${mediaPath}`);
    }
    let resolved;
    try {
        resolved = await fs__namespace.realpath(mediaPath);
    }
    catch {
        resolved = path__namespace.resolve(mediaPath);
    }
    for (const root of localRoots) {
        let resolvedRoot;
        try {
            resolvedRoot = await fs__namespace.realpath(root);
        }
        catch {
            resolvedRoot = path__namespace.resolve(root);
        }
        if (resolvedRoot === path__namespace.parse(resolvedRoot).root) {
            continue;
        }
        if (resolved === resolvedRoot || resolved.startsWith(resolvedRoot + path__namespace.sep)) {
            return;
        }
    }
    throw new Error(`Local media path is not under an allowed directory: ${mediaPath}`);
}
/** 从远程 URL 获取媒体 */
async function fetchRemoteMedia(url, maxBytes) {
    const res = await fetch(url, { redirect: "follow" });
    if (!res.ok) {
        throw new Error(`Failed to fetch media from ${url}: HTTP ${res.status} ${res.statusText}`);
    }
    const buffer = Buffer.from(await res.arrayBuffer());
    if (maxBytes && buffer.length > maxBytes) {
        throw new Error(`Media from ${url} exceeds max size (${buffer.length} > ${maxBytes})`);
    }
    const headerMime = res.headers.get("content-type")?.split(";")?.[0]?.trim();
    let fileName;
    const disposition = res.headers.get("content-disposition");
    if (disposition) {
        const match = /filename\*?\s*=\s*(?:UTF-8''|")?([^";]+)/i.exec(disposition);
        if (match?.[1]) {
            try {
                fileName = path__namespace.basename(decodeURIComponent(match[1].replace(/["']/g, "").trim()));
            }
            catch {
                fileName = path__namespace.basename(match[1].replace(/["']/g, "").trim());
            }
        }
    }
    if (!fileName) {
        try {
            const parsed = new URL(url);
            const base = path__namespace.basename(parsed.pathname);
            if (base && base.includes("."))
                fileName = base;
        }
        catch { /* ignore */ }
    }
    const contentType = await detectMimeFallback({ buffer, headerMime, filePath: fileName ?? url });
    return { buffer, contentType, fileName };
}
/** 展开 ~ 为用户主目录 */
function resolveUserPath(p) {
    if (p.startsWith("~")) {
        return path__namespace.join(os__namespace.homedir(), p.slice(1));
    }
    return p;
}
/** fallback 版 loadOutboundMediaFromUrl，参考 weclaw/src/web/media.ts */
async function loadOutboundMediaFromUrlFallback(mediaUrl, options = {}) {
    const { maxBytes, mediaLocalRoots } = options;
    // 去除 MEDIA: 前缀
    mediaUrl = mediaUrl.replace(/^\s*MEDIA\s*:\s*/i, "");
    // 处理 file:// URL
    if (mediaUrl.startsWith("file://")) {
        try {
            mediaUrl = node_url.fileURLToPath(mediaUrl);
        }
        catch {
            throw new Error(`Invalid file:// URL: ${mediaUrl}`);
        }
    }
    // 远程 URL
    if (/^https?:\/\//i.test(mediaUrl)) {
        const fetched = await fetchRemoteMedia(mediaUrl, maxBytes);
        return {
            buffer: fetched.buffer,
            contentType: fetched.contentType,
            fileName: fetched.fileName,
        };
    }
    // 展开 ~ 路径
    if (mediaUrl.startsWith("~")) {
        mediaUrl = resolveUserPath(mediaUrl);
    }
    // 本地文件：安全校验
    await assertLocalMediaAllowed(mediaUrl, mediaLocalRoots);
    // 读取本地文件
    let data;
    try {
        const stat = await fs__namespace.stat(mediaUrl);
        if (!stat.isFile()) {
            throw new Error(`Local media path is not a file: ${mediaUrl}`);
        }
        data = await fs__namespace.readFile(mediaUrl);
    }
    catch (err) {
        if (err?.code === "ENOENT") {
            throw new Error(`Local media file not found: ${mediaUrl}`);
        }
        throw err;
    }
    if (maxBytes && data.length > maxBytes) {
        throw new Error(`Local media exceeds max size (${data.length} > ${maxBytes})`);
    }
    const mime = await detectMimeFallback({ buffer: data, filePath: mediaUrl });
    const fileName = path__namespace.basename(mediaUrl) || undefined;
    return {
        buffer: data,
        contentType: mime,
        fileName,
    };
}
/**
 * 从 URL 或本地路径加载媒体文件（兼容入口）
 *
 * 优先使用 SDK 版本，不可用时使用 fallback。
 * SDK 版本抛出的业务异常（如 LocalMediaAccessError）会直接透传。
 */
async function loadOutboundMediaFromUrl(mediaUrl, options = {}) {
    const sdk = await _sdkReady;
    if (sdk.loadOutboundMediaFromUrl) {
        return sdk.loadOutboundMediaFromUrl(mediaUrl, options);
    }
    return loadOutboundMediaFromUrlFallback(mediaUrl, options);
}
// ============================================================================
// getDefaultMediaLocalRoots —— 获取默认媒体本地路径白名单
// ============================================================================
/** 解析 openclaw 状态目录 */
function resolveStateDir$1() {
    const stateOverride = process.env.OPENCLAW_STATE_DIR?.trim() || process.env.CLAWDBOT_STATE_DIR?.trim();
    if (stateOverride)
        return stateOverride;
    return path__namespace.join(os__namespace.homedir(), ".openclaw");
}
/**
 * 获取默认媒体本地路径白名单（兼容入口）
 *
 * 优先使用 SDK 版本，不可用时手动构建白名单（与 weclaw/src/media/local-roots.ts 逻辑一致）。
 */
async function getDefaultMediaLocalRoots() {
    const sdk = await _sdkReady;
    if (sdk.getDefaultMediaLocalRoots) {
        try {
            return sdk.getDefaultMediaLocalRoots();
        }
        catch {
            // SDK 版本异常，降级到 fallback
        }
    }
    // fallback: 手动构建默认白名单
    const stateDir = path__namespace.resolve(resolveStateDir$1());
    return [
        path__namespace.join(stateDir, "media"),
        path__namespace.join(stateDir, "agents"),
        path__namespace.join(stateDir, "workspace"),
        path__namespace.join(stateDir, "sandboxes"),
    ];
}

/**
 * 企业微信渠道常量定义
 */
/**
 * 企业微信渠道 ID
 */
const CHANNEL_ID = "wecom";
/**
 * 企业微信 WebSocket 命令枚举
 */
var WeComCommand;
(function (WeComCommand) {
    /** 认证订阅 */
    WeComCommand["SUBSCRIBE"] = "aibot_subscribe";
    /** 心跳 */
    WeComCommand["PING"] = "ping";
    /** 企业微信推送消息 */
    WeComCommand["AIBOT_CALLBACK"] = "aibot_callback";
    /** clawdbot 响应消息 */
    WeComCommand["AIBOT_RESPONSE"] = "aibot_response";
})(WeComCommand || (WeComCommand = {}));
// ============================================================================
// 超时和重试配置
// ============================================================================
/** 图片下载超时时间（毫秒） */
const IMAGE_DOWNLOAD_TIMEOUT_MS = 30000;
/** 文件下载超时时间（毫秒） */
const FILE_DOWNLOAD_TIMEOUT_MS = 60000;
/** 消息发送超时时间（毫秒） */
const REPLY_SEND_TIMEOUT_MS = 15000;
/** 消息处理总超时时间（毫秒） */
const MESSAGE_PROCESS_TIMEOUT_MS = 5 * 60 * 1000;
/** WebSocket 心跳间隔（毫秒） */
const WS_HEARTBEAT_INTERVAL_MS = 30000;
/** WebSocket 最大重连次数 */
const WS_MAX_RECONNECT_ATTEMPTS = 100;
// ============================================================================
// 消息状态管理配置
// ============================================================================
/** messageStates Map 条目的最大 TTL（毫秒），防止内存泄漏 */
const MESSAGE_STATE_TTL_MS = 10 * 60 * 1000;
/** messageStates Map 清理间隔（毫秒） */
const MESSAGE_STATE_CLEANUP_INTERVAL_MS = 60000;
/** messageStates Map 最大条目数 */
const MESSAGE_STATE_MAX_SIZE = 500;
// ============================================================================
// 消息模板
// ============================================================================
/** "思考中"流式消息占位内容 */
const THINKING_MESSAGE = "<think></think>";
/** 仅包含图片时的消息占位符 */
const MEDIA_IMAGE_PLACEHOLDER = "<media:image>";
/** 仅包含文件时的消息占位符 */
const MEDIA_DOCUMENT_PLACEHOLDER = "<media:document>";
// ============================================================================
// 默认值
// ============================================================================
// ============================================================================
// MCP 配置
// ============================================================================
/** 获取 MCP 配置的 WebSocket 命令 */
const MCP_GET_CONFIG_CMD = "aibot_get_mcp_config";
/** MCP 配置拉取超时时间（毫秒） */
const MCP_CONFIG_FETCH_TIMEOUT_MS = 15000;
const __filename$1 = url.fileURLToPath((typeof document === 'undefined' ? require('u' + 'rl').pathToFileURL(__filename).href : (_documentCurrentScript && _documentCurrentScript.tagName.toUpperCase() === 'SCRIPT' && _documentCurrentScript.src || new URL('index.cjs.js', document.baseURI).href)));
const __dirname$1 = path$1.dirname(__filename$1);
const PLUGIN_JSON_FILENAME = "openclaw.plugin.json";
path$1.resolve(path$1.join(__dirname$1, "../", PLUGIN_JSON_FILENAME));
// ============================================================================
// 默认值
// ============================================================================
/** 默认媒体大小上限（MB） */
const DEFAULT_MEDIA_MAX_MB = 5;
/** 文本分块大小上限 */
const TEXT_CHUNK_LIMIT = 4000;
// ============================================================================
// 媒体上传相关常量
// ============================================================================
/** 图片大小上限（字节）：10MB */
const IMAGE_MAX_BYTES = 10 * 1024 * 1024;
/** 视频大小上限（字节）：10MB */
const VIDEO_MAX_BYTES = 10 * 1024 * 1024;
/** 语音大小上限（字节）：2MB */
const VOICE_MAX_BYTES = 2 * 1024 * 1024;
/** 文件大小上限（字节）：20MB */
const FILE_MAX_BYTES = 20 * 1024 * 1024;
/** 文件绝对上限（字节）：超过此值无法发送，等于 FILE_MAX_BYTES */
const ABSOLUTE_MAX_BYTES = FILE_MAX_BYTES;

/**
 * 企业微信消息内容解析模块
 *
 * 负责从 WsFrame 中提取文本、图片、引用等内容
 */
// ============================================================================
// 解析函数
// ============================================================================
/**
 * 解析消息内容（支持单条消息、图文混排和引用消息）
 * @returns 提取的文本数组、图片URL数组和引用消息内容
 */
function parseMessageContent(body) {
    const textParts = [];
    const imageUrls = [];
    const imageAesKeys = new Map();
    const fileUrls = [];
    const fileAesKeys = new Map();
    let quoteContent;
    // 处理图文混排消息
    if (body.msgtype === "mixed" && body.mixed?.msg_item) {
        for (const item of body.mixed.msg_item) {
            if (item.msgtype === "text" && item.text?.content) {
                textParts.push(item.text.content);
            }
            else if (item.msgtype === "image" && item.image?.url) {
                imageUrls.push(item.image.url);
                if (item.image.aeskey) {
                    imageAesKeys.set(item.image.url, item.image.aeskey);
                }
            }
        }
    }
    else {
        // 处理单条消息
        if (body.text?.content) {
            textParts.push(body.text.content);
        }
        // 处理语音消息（语音转文字后的文本内容）
        if (body.msgtype === "voice" && body.voice?.content) {
            textParts.push(body.voice.content);
        }
        if (body.image?.url) {
            imageUrls.push(body.image.url);
            if (body.image.aeskey) {
                imageAesKeys.set(body.image.url, body.image.aeskey);
            }
        }
        // 处理文件消息
        if (body.msgtype === "file" && body.file?.url) {
            fileUrls.push(body.file.url);
            if (body.file.aeskey) {
                fileAesKeys.set(body.file.url, body.file.aeskey);
            }
        }
    }
    // 处理引用消息
    if (body.quote) {
        if (body.quote.msgtype === "text" && body.quote.text?.content) {
            quoteContent = body.quote.text.content;
        }
        else if (body.quote.msgtype === "voice" && body.quote.voice?.content) {
            quoteContent = body.quote.voice.content;
        }
        else if (body.quote.msgtype === "image" && body.quote.image?.url) {
            // 引用的图片消息：将图片 URL 加入下载列表
            imageUrls.push(body.quote.image.url);
            if (body.quote.image.aeskey) {
                imageAesKeys.set(body.quote.image.url, body.quote.image.aeskey);
            }
        }
        else if (body.quote.msgtype === "file" && body.quote.file?.url) {
            // 引用的文件消息：将文件 URL 加入下载列表
            fileUrls.push(body.quote.file.url);
            if (body.quote.file.aeskey) {
                fileAesKeys.set(body.quote.file.url, body.quote.file.aeskey);
            }
        }
    }
    return { textParts, imageUrls, imageAesKeys, fileUrls, fileAesKeys, quoteContent };
}

/**
 * 超时控制工具模块
 *
 * 为异步操作提供统一的超时保护机制
 */
/**
 * 为 Promise 添加超时保护
 *
 * @param promise - 原始 Promise
 * @param timeoutMs - 超时时间（毫秒）
 * @param message - 超时错误消息
 * @returns 带超时保护的 Promise
 */
function withTimeout(promise, timeoutMs, message) {
    if (timeoutMs <= 0 || !Number.isFinite(timeoutMs)) {
        return promise;
    }
    let timeoutId;
    const timeoutPromise = new Promise((_, reject) => {
        timeoutId = setTimeout(() => {
            reject(new TimeoutError(message ?? `Operation timed out after ${timeoutMs}ms`));
        }, timeoutMs);
    });
    return Promise.race([promise, timeoutPromise]).finally(() => {
        clearTimeout(timeoutId);
    });
}
/**
 * 超时错误类型
 */
class TimeoutError extends Error {
    constructor(message) {
        super(message);
        this.name = "TimeoutError";
    }
}

/**
 * 企业微信消息发送模块
 *
 * 负责通过 WSClient 发送回复消息，包含超时保护
 */
// ============================================================================
// 消息发送
// ============================================================================
/**
 * 发送企业微信回复消息
 * 供 monitor 内部和 channel outbound 使用
 *
 * @returns messageId (streamId)
 */
async function sendWeComReply(params) {
    const { wsClient, frame, text, runtime, finish = true, streamId: existingStreamId } = params;
    if (!text) {
        return "";
    }
    const streamId = existingStreamId || aibotNodeSdk.generateReqId("stream");
    if (!wsClient.isConnected) {
        runtime.error?.(`[wecom] WSClient not connected, cannot send reply`);
        throw new Error("WSClient not connected");
    }
    // 使用 SDK 的 replyStream 方法发送消息，带超时保护
    await withTimeout(wsClient.replyStream(frame, streamId, text, finish), REPLY_SEND_TIMEOUT_MS, `Reply send timed out (streamId=${streamId})`);
    runtime.log?.(`[plugin -> server] streamId=${streamId}, finish=${finish}, text=${text}`);
    return streamId;
}

/**
 * 企业微信媒体（图片）下载和保存模块
 *
 * 负责下载、检测格式、保存图片到本地，包含超时保护
 */
// ============================================================================
// 图片格式检测辅助函数（基于 file-type 包）
// ============================================================================
/**
 * 检查 Buffer 是否为有效的图片格式
 */
async function isImageBuffer(data) {
    const type = await fileType.fileTypeFromBuffer(data);
    return type?.mime.startsWith("image/") ?? false;
}
/**
 * 检测 Buffer 的图片内容类型
 */
async function detectImageContentType(data) {
    const type = await fileType.fileTypeFromBuffer(data);
    if (type?.mime.startsWith("image/")) {
        return type.mime;
    }
    return "application/octet-stream";
}
// ============================================================================
// 图片下载和保存
// ============================================================================
/**
 * 下载并保存所有图片到本地，每张图片的下载带超时保护
 */
async function downloadAndSaveImages(params) {
    const { imageUrls, config, runtime, wsClient } = params;
    const core = getWeComRuntime();
    const mediaList = [];
    for (const imageUrl of imageUrls) {
        try {
            runtime.log?.(`[wecom] Downloading image: url=${imageUrl}`);
            const mediaMaxMb = config.agents?.defaults?.mediaMaxMb ?? DEFAULT_MEDIA_MAX_MB;
            const maxBytes = mediaMaxMb * 1024 * 1024;
            let imageBuffer;
            let imageContentType;
            let originalFilename;
            const imageAesKey = params.imageAesKeys?.get(imageUrl);
            try {
                // 优先使用 SDK 的 downloadFile 方法下载（带超时保护）
                const result = await withTimeout(wsClient.downloadFile(imageUrl, imageAesKey), IMAGE_DOWNLOAD_TIMEOUT_MS, `Image download timed out: ${imageUrl}`);
                imageBuffer = result.buffer;
                originalFilename = result.filename;
                imageContentType = await detectImageContentType(imageBuffer);
                runtime.log?.(`[wecom] Image downloaded: size=${imageBuffer.length}, contentType=${imageContentType}, filename=${originalFilename ?? '(none)'}`);
            }
            catch (sdkError) {
                // 如果 SDK 方法失败，回退到原有方式（带超时保护）
                runtime.log?.(`[wecom] SDK download failed, fallback: ${String(sdkError)}`);
                const fetched = await withTimeout(core.channel.media.fetchRemoteMedia({ url: imageUrl }), IMAGE_DOWNLOAD_TIMEOUT_MS, `Manual image download timed out: ${imageUrl}`);
                runtime.log?.(`[wecom] Image fetched: contentType=${fetched.contentType}, size=${fetched.buffer.length}`);
                imageBuffer = fetched.buffer;
                imageContentType = fetched.contentType ?? "application/octet-stream";
                const isValidImage = await isImageBuffer(fetched.buffer);
                if (!isValidImage) {
                    runtime.log?.(`[wecom] WARN: Downloaded data is not a valid image format`);
                }
            }
            const saved = await core.channel.media.saveMediaBuffer(imageBuffer, imageContentType, "inbound", maxBytes, originalFilename);
            mediaList.push({ path: saved.path, contentType: saved.contentType });
            runtime.log?.(`[wecom][plugin] Image saved: path=${saved.path}, contentType=${saved.contentType}`);
        }
        catch (err) {
            runtime.error?.(`[wecom] Failed to download image: ${String(err)}`);
        }
    }
    return mediaList;
}
/**
 * 下载并保存所有文件到本地，每个文件的下载带超时保护
 */
async function downloadAndSaveFiles(params) {
    const { fileUrls, config, runtime, wsClient } = params;
    const core = getWeComRuntime();
    const mediaList = [];
    for (const fileUrl of fileUrls) {
        try {
            runtime.log?.(`[wecom] Downloading file: url=${fileUrl}`);
            const mediaMaxMb = config.agents?.defaults?.mediaMaxMb ?? DEFAULT_MEDIA_MAX_MB;
            const maxBytes = mediaMaxMb * 1024 * 1024;
            let fileBuffer;
            let fileContentType;
            let originalFilename;
            const fileAesKey = params.fileAesKeys?.get(fileUrl);
            try {
                // 使用 SDK 的 downloadFile 方法下载（带超时保护）
                const result = await withTimeout(wsClient.downloadFile(fileUrl, fileAesKey), FILE_DOWNLOAD_TIMEOUT_MS, `File download timed out: ${fileUrl}`);
                fileBuffer = result.buffer;
                originalFilename = result.filename;
                // 检测文件类型
                const type = await fileType.fileTypeFromBuffer(fileBuffer);
                fileContentType = type?.mime ?? "application/octet-stream";
                runtime.log?.(`[wecom] File downloaded: size=${fileBuffer.length}, contentType=${fileContentType}, filename=${originalFilename ?? '(none)'}`);
            }
            catch (sdkError) {
                // 如果 SDK 方法失败，回退到 fetchRemoteMedia（带超时保护）
                runtime.log?.(`[wecom] SDK file download failed, fallback: ${String(sdkError)}`);
                const fetched = await withTimeout(core.channel.media.fetchRemoteMedia({ url: fileUrl }), FILE_DOWNLOAD_TIMEOUT_MS, `Manual file download timed out: ${fileUrl}`);
                runtime.log?.(`[wecom] File fetched: contentType=${fetched.contentType}, size=${fetched.buffer.length}`);
                fileBuffer = fetched.buffer;
                fileContentType = fetched.contentType ?? "application/octet-stream";
            }
            const saved = await core.channel.media.saveMediaBuffer(fileBuffer, fileContentType, "inbound", maxBytes, originalFilename);
            mediaList.push({ path: saved.path, contentType: saved.contentType });
            runtime.log?.(`[wecom][plugin] File saved: path=${saved.path}, contentType=${saved.contentType}`);
        }
        catch (err) {
            runtime.error?.(`[wecom] Failed to download file: ${String(err)}`);
        }
    }
    return mediaList;
}

/**
 * 企业微信出站媒体上传工具模块
 *
 * 负责：
 * - 从 mediaUrl 加载文件 buffer（远程 URL 或本地路径均支持）
 * - 检测 MIME 类型并映射为企微媒体类型
 * - 文件大小检查与降级策略
 */
// ============================================================================
// MIME → 企微媒体类型映射
// ============================================================================
/**
 * 根据 MIME 类型检测企微媒体类型
 *
 * @param mimeType - MIME 类型字符串
 * @returns 企微媒体类型
 */
function detectWeComMediaType(mimeType) {
    const mime = mimeType.toLowerCase();
    // 图片类型
    if (mime.startsWith("image/")) {
        return "image";
    }
    // 视频类型
    if (mime.startsWith("video/")) {
        return "video";
    }
    // 语音类型
    if (mime.startsWith("audio/") ||
        mime === "application/ogg" // OGG 音频容器
    ) {
        return "voice";
    }
    // 其他类型默认为文件
    return "file";
}
// ============================================================================
// 媒体文件加载
// ============================================================================
/**
 * 从 mediaUrl 加载媒体文件
 *
 * 支持远程 URL（http/https）和本地路径（file:// 或绝对路径），
 * 利用 openclaw plugin-sdk 的 loadOutboundMediaFromUrl 统一处理。
 *
 * @param mediaUrl - 媒体文件的 URL 或本地路径
 * @param mediaLocalRoots - 允许读取本地文件的安全白名单目录
 * @returns 解析后的媒体文件信息
 */
async function resolveMediaFile(mediaUrl, mediaLocalRoots) {
    // 使用兼容层加载媒体文件（优先 SDK，不可用时 fallback）
    // 传入足够大的 maxBytes，由我们自己在后续步骤做大小检查
    const result = await loadOutboundMediaFromUrl(mediaUrl, {
        maxBytes: ABSOLUTE_MAX_BYTES,
        mediaLocalRoots,
    });
    if (!result.buffer || result.buffer.length === 0) {
        throw new Error(`Failed to load media from ${mediaUrl}: empty buffer`);
    }
    // 检测真实 MIME 类型
    let contentType = result.contentType || "application/octet-stream";
    // 如果没有返回准确的 contentType，尝试通过 buffer 魔术字节检测
    if (contentType === "application/octet-stream" ||
        contentType === "text/plain") {
        const detected = await detectMime(result.buffer);
        if (detected) {
            contentType = detected;
        }
    }
    // 提取文件名
    const fileName = extractFileName(mediaUrl, result.fileName, contentType);
    return {
        buffer: result.buffer,
        contentType,
        fileName,
    };
}
// ============================================================================
// 文件大小检查与降级
// ============================================================================
/** 企微语音消息仅支持 AMR 格式 */
const VOICE_SUPPORTED_MIMES = new Set(["audio/amr"]);
/**
 * 检查文件大小并执行降级策略
 *
 * 降级规则：
 * - voice 非 AMR 格式 → 降级为 file（企微后台仅支持 AMR）
 * - image 超过 10MB → 降级为 file
 * - video 超过 10MB → 降级为 file
 * - voice 超过 2MB → 降级为 file
 * - file 超过 20MB → 拒绝发送
 *
 * @param fileSize - 文件大小（字节）
 * @param detectedType - 检测到的企微媒体类型
 * @param contentType - 文件的 MIME 类型（用于语音格式校验）
 * @returns 大小检查结果
 */
function applyFileSizeLimits(fileSize, detectedType, contentType) {
    const fileSizeMB = (fileSize / (1024 * 1024)).toFixed(2);
    // 先检查绝对上限（20MB）
    if (fileSize > ABSOLUTE_MAX_BYTES) {
        return {
            finalType: detectedType,
            shouldReject: true,
            rejectReason: `文件大小 ${fileSizeMB}MB 超过了企业微信允许的最大限制 20MB，无法发送。请尝试压缩文件或减小文件大小。`,
            downgraded: false,
        };
    }
    // 按类型检查大小限制
    switch (detectedType) {
        case "image":
            if (fileSize > IMAGE_MAX_BYTES) {
                return {
                    finalType: "file",
                    shouldReject: false,
                    downgraded: true,
                    downgradeNote: `图片大小 ${fileSizeMB}MB 超过 10MB 限制，已转为文件格式发送`,
                };
            }
            break;
        case "video":
            if (fileSize > VIDEO_MAX_BYTES) {
                return {
                    finalType: "file",
                    shouldReject: false,
                    downgraded: true,
                    downgradeNote: `视频大小 ${fileSizeMB}MB 超过 10MB 限制，已转为文件格式发送`,
                };
            }
            break;
        case "voice":
            // 企微语音消息仅支持 AMR 格式，非 AMR 一律降级为文件
            if (contentType && !VOICE_SUPPORTED_MIMES.has(contentType.toLowerCase())) {
                return {
                    finalType: "file",
                    shouldReject: false,
                    downgraded: true,
                    downgradeNote: `语音格式 ${contentType} 不支持，企微仅支持 AMR 格式，已转为文件格式发送`,
                };
            }
            if (fileSize > VOICE_MAX_BYTES) {
                return {
                    finalType: "file",
                    shouldReject: false,
                    downgraded: true,
                    downgradeNote: `语音大小 ${fileSizeMB}MB 超过 2MB 限制，已转为文件格式发送`,
                };
            }
            break;
    }
    // 无需降级
    return {
        finalType: detectedType,
        shouldReject: false,
        downgraded: false,
    };
}
// ============================================================================
// 辅助函数
// ============================================================================
/**
 * 从 URL/路径中提取文件名
 */
function extractFileName(mediaUrl, providedFileName, contentType) {
    // 优先使用提供的文件名
    if (providedFileName) {
        return providedFileName;
    }
    // 尝试从 URL 中提取
    try {
        const urlObj = new URL(mediaUrl, "file://");
        const pathParts = urlObj.pathname.split("/");
        const lastPart = pathParts[pathParts.length - 1];
        if (lastPart && lastPart.includes(".")) {
            return decodeURIComponent(lastPart);
        }
    }
    catch {
        // 尝试作为普通路径处理
        const parts = mediaUrl.split("/");
        const lastPart = parts[parts.length - 1];
        if (lastPart && lastPart.includes(".")) {
            return lastPart;
        }
    }
    // 使用 MIME 类型生成默认文件名
    const ext = mimeToExtension(contentType || "application/octet-stream");
    return `media_${Date.now()}${ext}`;
}
/**
 * MIME 类型转文件扩展名
 */
function mimeToExtension(mime) {
    const map = {
        "image/jpeg": ".jpg",
        "image/png": ".png",
        "image/gif": ".gif",
        "image/webp": ".webp",
        "image/bmp": ".bmp",
        "image/svg+xml": ".svg",
        "video/mp4": ".mp4",
        "video/quicktime": ".mov",
        "video/x-msvideo": ".avi",
        "video/webm": ".webm",
        "audio/mpeg": ".mp3",
        "audio/ogg": ".ogg",
        "audio/wav": ".wav",
        "audio/amr": ".amr",
        "audio/aac": ".aac",
        "application/pdf": ".pdf",
        "application/zip": ".zip",
        "application/msword": ".doc",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document": ".docx",
        "application/vnd.ms-excel": ".xls",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": ".xlsx",
        "text/plain": ".txt",
    };
    return map[mime] || ".bin";
}
/**
 * 公共媒体上传+发送流程
 *
 * 统一处理：resolveMediaFile → detectType → sizeCheck → uploadMedia → sendMediaMessage
 * 媒体消息统一走 aibot_send_msg 主动发送，避免多文件场景下 reqId 只能用一次的问题。
 * channel.ts 的 sendMedia 和 monitor.ts 的 deliver 回调都使用此函数。
 */
async function uploadAndSendMedia(options) {
    const { wsClient, mediaUrl, chatId, mediaLocalRoots, log, errorLog } = options;
    try {
        // 1. 加载媒体文件
        log?.(`[wecom] Uploading media: url=${mediaUrl}`);
        const media = await resolveMediaFile(mediaUrl, mediaLocalRoots);
        // 2. 检测企微媒体类型
        const detectedType = detectWeComMediaType(media.contentType);
        // 3. 文件大小检查与降级策略
        const sizeCheck = applyFileSizeLimits(media.buffer.length, detectedType, media.contentType);
        if (sizeCheck.shouldReject) {
            errorLog?.(`[wecom] Media rejected: ${sizeCheck.rejectReason}`);
            return {
                ok: false,
                rejected: true,
                rejectReason: sizeCheck.rejectReason,
                finalType: sizeCheck.finalType,
            };
        }
        const finalType = sizeCheck.finalType;
        // 4. 分片上传获取 media_id
        const uploadResult = await wsClient.uploadMedia(media.buffer, {
            type: finalType,
            filename: media.fileName,
        });
        log?.(`[wecom] Media uploaded: media_id=${uploadResult.media_id}, type=${finalType}`);
        // 5. 统一通过 aibot_send_msg 主动发送媒体消息
        const result = await wsClient.sendMediaMessage(chatId, finalType, uploadResult.media_id);
        const messageId = result?.headers?.req_id ?? `wecom-media-${Date.now()}`;
        log?.(`[wecom] Media sent via sendMediaMessage: chatId=${chatId}, type=${finalType}`);
        return {
            ok: true,
            messageId,
            finalType,
            downgraded: sizeCheck.downgraded,
            downgradeNote: sizeCheck.downgradeNote,
        };
    }
    catch (err) {
        const errMsg = String(err);
        errorLog?.(`[wecom] Failed to upload/send media: url=${mediaUrl}, error=${errMsg}`);
        return {
            ok: false,
            error: errMsg,
        };
    }
}

/**
 * 企业微信群组访问控制模块
 *
 * 负责群组策略检查（groupPolicy、群组白名单、群内发送者白名单）
 */
// ============================================================================
// 内部辅助函数
// ============================================================================
/**
 * 解析企业微信群组配置
 */
function resolveWeComGroupConfig(params) {
    const groups = params.cfg?.groups ?? {};
    const wildcard = groups["*"];
    const groupId = params.groupId?.trim();
    if (!groupId) {
        return undefined;
    }
    const direct = groups[groupId];
    if (direct) {
        return direct;
    }
    const lowered = groupId.toLowerCase();
    const matchKey = Object.keys(groups).find((key) => key.toLowerCase() === lowered);
    if (matchKey) {
        return groups[matchKey];
    }
    return wildcard;
}
/**
 * 检查群组是否在允许列表中
 */
function isWeComGroupAllowed(params) {
    const { groupPolicy } = params;
    if (groupPolicy === "disabled") {
        return false;
    }
    if (groupPolicy === "open") {
        return true;
    }
    // allowlist 模式：检查群组是否在允许列表中
    const normalizedAllowFrom = params.allowFrom.map((entry) => String(entry).replace(new RegExp(`^${CHANNEL_ID}:`, "i"), "").trim());
    if (normalizedAllowFrom.includes("*")) {
        return true;
    }
    const normalizedGroupId = params.groupId.trim();
    return normalizedAllowFrom.some((entry) => entry === normalizedGroupId || entry.toLowerCase() === normalizedGroupId.toLowerCase());
}
/**
 * 检查群组内发送者是否在允许列表中
 */
function isGroupSenderAllowed(params) {
    const { senderId, groupId, wecomConfig } = params;
    const groupConfig = resolveWeComGroupConfig({
        cfg: wecomConfig,
        groupId,
    });
    const perGroupSenderAllowFrom = (groupConfig?.allowFrom ?? []).map((v) => String(v));
    if (perGroupSenderAllowFrom.length === 0) {
        return true;
    }
    if (perGroupSenderAllowFrom.includes("*")) {
        return true;
    }
    return perGroupSenderAllowFrom.some((entry) => {
        const normalized = entry.replace(new RegExp(`^${CHANNEL_ID}:`, "i"), "").trim();
        return normalized === senderId || normalized === `user:${senderId}`;
    });
}
// ============================================================================
// 公开 API
// ============================================================================
/**
 * 检查群组策略访问控制
 * @returns 检查结果，包含是否允许继续处理
 */
function checkGroupPolicy(params) {
    const { chatId, senderId, account, config, runtime } = params;
    const wecomConfig = (config.channels?.[CHANNEL_ID] ?? {});
    const defaultGroupPolicy = config.channels?.[CHANNEL_ID]?.groupPolicy;
    const groupPolicy = account.config.groupPolicy ?? defaultGroupPolicy ?? "open";
    // const { groupPolicy, providerMissingFallbackApplied } = resolveOpenProviderRuntimeGroupPolicy({
    //   providerConfigPresent: config.channels?.[CHANNEL_ID] !== undefined,
    //   groupPolicy: wecomConfig.groupPolicy,
    //   defaultGroupPolicy,
    // });
    // warnMissingProviderGroupPolicyFallbackOnce({
    //   providerMissingFallbackApplied,
    //   providerKey: CHANNEL_ID,
    //   accountId: account.accountId,
    //   log: (msg) => runtime.log?.(msg),
    // });
    const groupAllowFrom = wecomConfig.groupAllowFrom ?? [];
    const groupAllowed = isWeComGroupAllowed({
        groupPolicy,
        allowFrom: groupAllowFrom,
        groupId: chatId,
    });
    if (!groupAllowed) {
        runtime.log?.(`[WeCom] Group ${chatId} not allowed (groupPolicy=${groupPolicy})`);
        return { allowed: false };
    }
    const senderAllowed = isGroupSenderAllowed({
        senderId,
        groupId: chatId,
        wecomConfig,
    });
    if (!senderAllowed) {
        runtime.log?.(`[WeCom] Sender ${senderId} not in group ${chatId} sender allowlist`);
        return { allowed: false };
    }
    return { allowed: true };
}
/**
 * 检查发送者是否在允许列表中（通用）
 */
function isSenderAllowed(senderId, allowFrom) {
    if (allowFrom.includes("*")) {
        return true;
    }
    return allowFrom.some((entry) => {
        const normalized = entry.replace(new RegExp(`^${CHANNEL_ID}:`, "i"), "").trim();
        return normalized === senderId || normalized === `user:${senderId}`;
    });
}

/**
 * 企业微信 DM（私聊）访问控制模块
 *
 * 负责私聊策略检查、配对流程
 */
// ============================================================================
// 公开 API
// ============================================================================
/**
 * 检查 DM Policy 访问控制
 * @returns 检查结果，包含是否允许继续处理
 */
async function checkDmPolicy(params) {
    const { senderId, isGroup, account, wsClient, frame, runtime } = params;
    const core = getWeComRuntime();
    // 群聊消息不检查 DM Policy
    if (isGroup) {
        return { allowed: true };
    }
    const dmPolicy = account.config.dmPolicy ?? "open";
    const configAllowFrom = (account.config.allowFrom ?? []).map((v) => String(v));
    // 如果 dmPolicy 是 disabled，直接拒绝
    if (dmPolicy === "disabled") {
        runtime.log?.(`[WeCom] Blocked DM from ${senderId} (dmPolicy=disabled)`);
        return { allowed: false };
    }
    // 如果是 open 模式，允许所有人
    if (dmPolicy === "open") {
        return { allowed: true };
    }
    // OpenClaw <= 2026.2.19 signature: readAllowFromStore(channel, env?, accountId?)
    const oldStoreAllowFrom = await core.channel.pairing.readAllowFromStore('wecom', undefined, account.accountId).catch(() => []);
    // Compatibility fallback for newer OpenClaw implementations.
    const newStoreAllowFrom = await core.channel.pairing
        .readAllowFromStore({ channel: CHANNEL_ID, accountId: account.accountId })
        .catch(() => []);
    // 检查发送者是否在允许列表中
    const storeAllowFrom = [...oldStoreAllowFrom, ...newStoreAllowFrom];
    const effectiveAllowFrom = [...configAllowFrom, ...storeAllowFrom];
    const senderAllowedResult = isSenderAllowed(senderId, effectiveAllowFrom);
    if (senderAllowedResult) {
        return { allowed: true };
    }
    // 处理未授权用户
    if (dmPolicy === "pairing") {
        const { code, created } = await core.channel.pairing.upsertPairingRequest({
            channel: CHANNEL_ID,
            id: senderId,
            accountId: account.accountId,
            meta: { name: senderId },
        });
        if (created) {
            runtime.log?.(`[WeCom] Pairing request created for sender=${senderId}`);
            try {
                await sendWeComReply({
                    wsClient,
                    frame,
                    text: core.channel.pairing.buildPairingReply({
                        channel: CHANNEL_ID,
                        idLine: `您的企业微信用户ID: ${senderId}`,
                        code,
                    }),
                    runtime,
                    finish: true,
                });
            }
            catch (err) {
                runtime.error?.(`[WeCom] Failed to send pairing reply to ${senderId}: ${String(err)}`);
            }
        }
        else {
            runtime.log?.(`[WeCom] Pairing request already exists for sender=${senderId}`);
        }
        return { allowed: false, pairingSent: created };
    }
    // allowlist 模式：直接拒绝未授权用户
    runtime.log?.(`[WeCom] Blocked unauthorized sender ${senderId} (dmPolicy=${dmPolicy})`);
    return { allowed: false };
}

// ============================================================================
// 常量
// ============================================================================
const DEFAULT_TTL_MS = 7 * 24 * 60 * 60 * 1000; // 7 天
const DEFAULT_MEMORY_MAX_SIZE = 200;
const DEFAULT_FILE_MAX_ENTRIES = 500;
const DEFAULT_FLUSH_DEBOUNCE_MS = 1000;
const DEFAULT_LOCK_OPTIONS = {
    stale: 60000,
    retries: {
        retries: 6,
        factor: 1.35,
        minTimeout: 8,
        maxTimeout: 180,
        randomize: true,
    },
};
// ============================================================================
// 状态目录解析
// ============================================================================
function resolveStateDirFromEnv(env = process.env) {
    const stateOverride = env.OPENCLAW_STATE_DIR?.trim() || env.CLAWDBOT_STATE_DIR?.trim();
    if (stateOverride) {
        return stateOverride;
    }
    if (env.VITEST || env.NODE_ENV === "test") {
        return path.join(os.tmpdir(), ["openclaw-vitest", String(process.pid)].join("-"));
    }
    return path.join(os.homedir(), ".openclaw");
}
function resolveReqIdFilePath(accountId) {
    const safe = accountId.replace(/[^a-zA-Z0-9_-]/g, "_");
    return path.join(resolveStateDirFromEnv(), "wecom", `reqid-map-${safe}.json`);
}
// ============================================================================
// 核心实现
// ============================================================================
function createPersistentReqIdStore(accountId, options) {
    const ttlMs = DEFAULT_TTL_MS;
    const memoryMaxSize = DEFAULT_MEMORY_MAX_SIZE;
    const fileMaxEntries = DEFAULT_FILE_MAX_ENTRIES;
    const flushDebounceMs = DEFAULT_FLUSH_DEBOUNCE_MS;
    const filePath = resolveReqIdFilePath(accountId);
    // 内存层：chatId → ReqIdEntry
    const memory = new Map();
    // 防抖写入相关
    let dirty = false;
    let flushTimer = null;
    // ========== 内部辅助函数 ==========
    /** 检查条目是否过期 */
    function isExpired(entry, now) {
        return now - entry.ts >= ttlMs;
    }
    /** 验证磁盘条目的合法性 */
    function isValidEntry(entry) {
        return (typeof entry === "object" &&
            entry !== null &&
            typeof entry.reqId === "string" &&
            typeof entry.ts === "number" &&
            Number.isFinite(entry.ts));
    }
    /** 清理磁盘数据中的无效值，返回干净的 Record */
    function sanitizeData(value) {
        if (!value || typeof value !== "object") {
            return {};
        }
        const out = {};
        for (const [key, entry] of Object.entries(value)) {
            if (isValidEntry(entry)) {
                out[key] = entry;
            }
        }
        return out;
    }
    /**
     * 内存容量控制：淘汰最旧的条目。
     * 利用 Map 的插入顺序 + touch(先 delete 再 set) 实现类 LRU 效果。
     */
    function pruneMemory() {
        if (memory.size <= memoryMaxSize)
            return;
        const sorted = [...memory.entries()].sort((a, b) => a[1].ts - b[1].ts);
        const toRemove = sorted.slice(0, memory.size - memoryMaxSize);
        for (const [key] of toRemove) {
            memory.delete(key);
        }
    }
    /** 磁盘数据容量控制：先清过期，再按时间淘汰超量 */
    function pruneFileData(data, now) {
        {
            for (const [key, entry] of Object.entries(data)) {
                if (now - entry.ts >= ttlMs) {
                    delete data[key];
                }
            }
        }
        const keys = Object.keys(data);
        if (keys.length <= fileMaxEntries)
            return;
        keys
            .sort((a, b) => data[a].ts - data[b].ts)
            .slice(0, keys.length - fileMaxEntries)
            .forEach((key) => delete data[key]);
    }
    /** 防抖写入磁盘 */
    function scheduleDiskFlush() {
        dirty = true;
        if (flushTimer)
            return;
        flushTimer = setTimeout(async () => {
            flushTimer = null;
            if (!dirty)
                return;
            await flushToDisk();
        }, flushDebounceMs);
    }
    /** 立即写入磁盘（带文件锁，参考 createPersistentDedupe 的 checkAndRecordInner） */
    async function flushToDisk() {
        dirty = false;
        const now = Date.now();
        try {
            await pluginSdk.withFileLock(filePath, DEFAULT_LOCK_OPTIONS, async () => {
                // 读取现有磁盘数据并合并
                const { value } = await pluginSdk.readJsonFileWithFallback(filePath, {});
                const data = sanitizeData(value);
                // 将内存中未过期的数据合并到磁盘数据（内存优先）
                for (const [chatId, entry] of memory) {
                    if (!isExpired(entry, now)) {
                        data[chatId] = entry;
                    }
                }
                // 清理过期和超量
                pruneFileData(data, now);
                // 原子写入
                await pluginSdk.writeJsonFileAtomically(filePath, data);
            });
        }
        catch (error) {
            // 磁盘写入失败不影响内存使用，降级到纯内存模式
            // console.error(`[WeCom] reqid-store: flush to disk failed: ${String(error)}`);
        }
    }
    // ========== 公开 API ==========
    function set(chatId, reqId) {
        const entry = { reqId, ts: Date.now() };
        // touch：先删再设，保持 Map 插入顺序（类 LRU）
        memory.delete(chatId);
        memory.set(chatId, entry);
        pruneMemory();
        scheduleDiskFlush();
    }
    async function get(chatId) {
        const now = Date.now();
        // 1. 先查内存
        const memEntry = memory.get(chatId);
        if (memEntry && !isExpired(memEntry, now)) {
            return memEntry.reqId;
        }
        if (memEntry) {
            memory.delete(chatId); // 过期则删除
        }
        // 2. 内存 miss，回查磁盘并回填内存
        try {
            const { value } = await pluginSdk.readJsonFileWithFallback(filePath, {});
            const data = sanitizeData(value);
            const diskEntry = data[chatId];
            if (diskEntry && !isExpired(diskEntry, now)) {
                // 回填内存
                memory.set(chatId, diskEntry);
                return diskEntry.reqId;
            }
        }
        catch {
            // 磁盘读取失败，降级返回 undefined
        }
        return undefined;
    }
    function getSync(chatId) {
        const now = Date.now();
        const entry = memory.get(chatId);
        if (entry && !isExpired(entry, now)) {
            return entry.reqId;
        }
        if (entry) {
            memory.delete(chatId);
        }
        return undefined;
    }
    function del(chatId) {
        memory.delete(chatId);
        scheduleDiskFlush();
    }
    async function warmup(onError) {
        const now = Date.now();
        try {
            const { value } = await pluginSdk.readJsonFileWithFallback(filePath, {});
            const data = sanitizeData(value);
            let loaded = 0;
            for (const [chatId, entry] of Object.entries(data)) {
                if (!isExpired(entry, now)) {
                    memory.set(chatId, entry);
                    loaded++;
                }
            }
            pruneMemory();
            return loaded;
        }
        catch (error) {
            onError?.(error);
            return 0;
        }
    }
    async function flush() {
        if (flushTimer) {
            clearTimeout(flushTimer);
            flushTimer = null;
        }
        await flushToDisk();
    }
    function clearMemory() {
        memory.clear();
    }
    function memorySize() {
        return memory.size;
    }
    return {
        set,
        get,
        getSync,
        delete: del,
        warmup,
        flush,
        clearMemory,
        memorySize,
    };
}

/**
 * 企业微信全局状态管理模块
 *
 * 负责管理 WSClient 实例、消息状态（带 TTL 清理）、ReqId 存储
 * 解决全局 Map 的内存泄漏问题
 */
// ============================================================================
// WSClient 实例管理
// ============================================================================
/** WSClient 实例管理 */
const wsClientInstances = new Map();
/**
 * 获取指定账户的 WSClient 实例
 */
function getWeComWebSocket(accountId) {
    return wsClientInstances.get(accountId) ?? null;
}
/**
 * 设置指定账户的 WSClient 实例
 */
function setWeComWebSocket(accountId, client) {
    wsClientInstances.set(accountId, client);
}
/** 消息状态管理 */
const messageStates = new Map();
/** 定期清理定时器 */
let cleanupTimer = null;
/**
 * 启动消息状态定期清理（自动 TTL 清理 + 容量限制）
 */
function startMessageStateCleanup() {
    if (cleanupTimer)
        return;
    cleanupTimer = setInterval(() => {
        pruneMessageStates();
    }, MESSAGE_STATE_CLEANUP_INTERVAL_MS);
    // 允许进程退出时不阻塞
    if (cleanupTimer && typeof cleanupTimer === "object" && "unref" in cleanupTimer) {
        cleanupTimer.unref();
    }
}
/**
 * 停止消息状态定期清理
 */
function stopMessageStateCleanup() {
    if (cleanupTimer) {
        clearInterval(cleanupTimer);
        cleanupTimer = null;
    }
}
/**
 * 清理过期和超量的消息状态条目
 */
function pruneMessageStates() {
    const now = Date.now();
    // 1. 清理过期条目
    for (const [key, entry] of messageStates) {
        if (now - entry.createdAt >= MESSAGE_STATE_TTL_MS) {
            messageStates.delete(key);
        }
    }
    // 2. 容量限制：如果仍超过最大条目数，按时间淘汰最旧的
    if (messageStates.size > MESSAGE_STATE_MAX_SIZE) {
        const sorted = [...messageStates.entries()].sort((a, b) => a[1].createdAt - b[1].createdAt);
        const toRemove = sorted.slice(0, messageStates.size - MESSAGE_STATE_MAX_SIZE);
        for (const [key] of toRemove) {
            messageStates.delete(key);
        }
    }
}
/**
 * 设置消息状态
 */
function setMessageState(messageId, state) {
    messageStates.set(messageId, {
        state,
        createdAt: Date.now(),
    });
}
/**
 * 删除消息状态
 */
function deleteMessageState(messageId) {
    messageStates.delete(messageId);
}
// ============================================================================
// ReqId 持久化存储管理（按 accountId 隔离）
// ============================================================================
/**
 * ReqId 持久化存储管理
 * 参考 createPersistentDedupe 模式：内存 + 磁盘双层、文件锁、原子写入、TTL 过期、防抖写入
 * 重启后可从磁盘恢复，确保主动推送消息时能获取到 reqId
 */
const reqIdStores = new Map();
function getOrCreateReqIdStore(accountId) {
    let store = reqIdStores.get(accountId);
    if (!store) {
        store = createPersistentReqIdStore(accountId);
        reqIdStores.set(accountId, store);
    }
    return store;
}
// ============================================================================
// ReqId 操作函数
// ============================================================================
/**
 * 设置 chatId 对应的 reqId（写入内存 + 防抖写磁盘）
 */
function setReqIdForChat(chatId, reqId, accountId = "default") {
    getOrCreateReqIdStore(accountId).set(chatId, reqId);
}
/**
 * 启动时预热 reqId 缓存（从磁盘加载到内存）
 */
async function warmupReqIdStore(accountId = "default", log) {
    const store = getOrCreateReqIdStore(accountId);
    return store.warmup((error) => {
        log?.(`[WeCom] reqid-store warmup error: ${String(error)}`);
    });
}
// ============================================================================
// 全局 cleanup（断开连接时释放所有资源）
// ============================================================================
/**
 * 清理指定账户的所有资源
 */
async function cleanupAccount(accountId) {
    // 1. 断开 WSClient
    const wsClient = wsClientInstances.get(accountId);
    if (wsClient) {
        try {
            wsClient.disconnect();
        }
        catch {
            // 忽略断开连接时的错误
        }
        wsClientInstances.delete(accountId);
    }
    // 2. flush reqId 存储到磁盘
    const store = reqIdStores.get(accountId);
    if (store) {
        try {
            await store.flush();
        }
        catch {
            // 忽略 flush 错误
        }
        // 注意：不删除 store，因为重连后可能还需要
    }
}

/**
 * MCP 配置拉取与持久化模块
 *
 * 负责:
 * - 通过 WSClient 发送 aibot_get_mcp_config 请求
 * - 解析服务端响应,提取 MCP 配置(url、type、is_authed)
 * - 将配置写入 openclaw.plugin.json 的 wecomMcp 字段
 */
// ============================================================================
// MCP 配置拉取
// ============================================================================
/**
 * 通过 WSClient 发送 aibot_get_mcp_config 命令,获取 MCP 配置
 *
 * @param wsClient - 已认证的 WSClient 实例
 * @returns MCP 配置(url、type、is_authed)
 * @throws 响应错误码非 0 或缺少 url 字段时抛出错误
 */
async function fetchMcpConfig(wsClient) {
    const biz_type = "doc";
    const reqId = aibotNodeSdk.generateReqId("mcp_config");
    // 通过 reply 方法发送自定义命令
    const response = await withTimeout(wsClient.reply({ headers: { req_id: reqId } }, { biz_type }, MCP_GET_CONFIG_CMD), MCP_CONFIG_FETCH_TIMEOUT_MS, `MCP config fetch timed out after ${MCP_CONFIG_FETCH_TIMEOUT_MS}ms`);
    // 校验响应错误码
    if (response.errcode && response.errcode !== 0) {
        throw new Error(`MCP config request failed: errcode=${response.errcode}, errmsg=${response.errmsg ?? "unknown"}`);
    }
    // 提取并校验 body
    const body = response.body;
    if (!body?.url) {
        throw new Error("MCP config response missing required 'url' field");
    }
    return {
        biz_type,
        url: body.url,
        type: body.type,
        is_authed: body.is_authed,
    };
}
// ============================================================================
// 配置持久化
// ============================================================================
/**
 * 将 MCP 配置写入 openclaw.plugin.json 的 mcpConfig 字段
 *
 * 使用 OpenClaw SDK 提供的文件锁和原子写入,保证并发安全。
 * 配置格式: { mcpConfig: { [type]: { type, url } } }
 * 示例: { "mcpConfig": { "doc": { "type": "streamable-http", "url": "..." } } }
 *
 * @param config - MCP 配置
 * @param runtime - 运行时环境
 */
async function saveMcpConfigToPluginJson(config, runtime) {
    //   const pluginJsonPath = PLUGIN_JSON_PATH;
    // 获取绝对路径并写入到配置文件
    //   const absolutePath = path.resolve(pluginJsonPath);
    const wecomConfigDir = path$1.join(os$1.homedir(), ".openclaw", "wecomConfig");
    const wecomConfigPath = path$1.join(wecomConfigDir, "config.json");
    //   // 写入 wecom 配置文件
    //   try {
    //     const lockOptions = {
    //       stale: 60_000,
    //       retries: {
    //         retries: 3,
    //         factor: 1.35,
    //         minTimeout: 8,
    //         maxTimeout: 200,
    //         randomize: true,
    //       },
    //     };
    //     await withFileLock(wecomConfigPath, lockOptions, async () => {
    //       const { value: wecomConfig } = await readJsonFileWithFallback<Record<string, unknown>>(
    //         wecomConfigPath,
    //         {},
    //       );
    //       wecomConfig.pluginConfigPath = absolutePath;
    //       await writeJsonFileAtomically(wecomConfigPath, wecomConfig);
    //       runtime.log?.(`[WeCom] Plugin config path saved to ${wecomConfigPath}`);
    //     });
    //   } catch (err) {
    //     runtime.error?.(`[WeCom] Failed to save plugin config path: ${String(err)}`);
    //   }
    const lockOptions = {
        stale: 60000, // 60秒锁过期
        retries: {
            retries: 6,
            factor: 1.35,
            minTimeout: 8,
            maxTimeout: 1200,
            randomize: true,
        },
    };
    await pluginSdk.withFileLock(wecomConfigPath, lockOptions, async () => {
        // 读取现有配置(不存在时使用空对象)
        const { value: pluginJson } = await pluginSdk.readJsonFileWithFallback(wecomConfigPath, {});
        // 确保 mcpConfig 字段存在且为对象
        if (!pluginJson.mcpConfig || typeof pluginJson.mcpConfig !== 'object') {
            pluginJson.mcpConfig = {};
        }
        // 使用 type 作为键存储配置
        const typeKey = config.biz_type || 'default';
        pluginJson.mcpConfig[typeKey] = {
            type: config.type,
            url: config.url,
        };
        // 原子写入
        await pluginSdk.writeJsonFileAtomically(wecomConfigPath, pluginJson);
        runtime.log?.(`[WeCom] MCP config saved to ${wecomConfigPath}`);
    });
}
// ============================================================================
// 组合入口
// ============================================================================
/**
 * 拉取 MCP 配置并持久化到 openclaw.plugin.json
 *
 * 认证成功后调用。失败仅记录日志,不影响 WebSocket 消息正常收发。
 *
 * @param wsClient - 已认证的 WSClient 实例
 * @param accountId - 账户 ID(用于日志)
 * @param runtime - 运行时环境(用于日志)
 */
async function fetchAndSaveMcpConfig(wsClient, accountId, runtime) {
    try {
        runtime.log?.(`[${accountId}] Fetching MCP config...`);
        const config = await fetchMcpConfig(wsClient);
        runtime.log?.(`[${accountId}] MCP config fetched: url=${config.url}, type=${config.type ?? "N/A"}, is_authed=${config.is_authed ?? "N/A"}`);
        await saveMcpConfigToPluginJson(config, runtime);
    }
    catch (err) {
        runtime.error?.(`[${accountId}] Failed to fetch/save MCP config: ${String(err)}`);
    }
}

/**
 * 企业微信 WebSocket 监控器主模块
 *
 * 负责：
 * - 建立和管理 WebSocket 连接
 * - 协调消息处理流程（解析→策略检查→下载图片→路由回复）
 * - 资源生命周期管理
 *
 * 子模块：
 * - message-parser.ts  : 消息内容解析
 * - message-sender.ts  : 消息发送（带超时保护）
 * - media-handler.ts   : 图片下载和保存（带超时保护）
 * - group-policy.ts    : 群组访问控制
 * - dm-policy.ts       : 私聊访问控制
 * - state-manager.ts   : 全局状态管理（带 TTL 清理）
 * - timeout.ts         : 超时工具
 */
/**
 * 去除文本中的 `<think>...</think>` 标签（支持跨行），返回剩余可见文本。
 * 用于判断大模型回复中是否包含实际用户可见内容（而非仅有 thinking 推理过程）。
 */
function stripThinkTags(text) {
    return text;
    // return text.replace(/<think>[\s\S]*?<\/think>/g, "").trim();
}
// ============================================================================
// 媒体本地路径白名单扩展
// ============================================================================
/**
 * 解析 openclaw 状态目录（与 plugin-sdk 内部逻辑保持一致）
 */
function resolveStateDir() {
    const stateOverride = process.env.OPENCLAW_STATE_DIR?.trim() || process.env.CLAWDBOT_STATE_DIR?.trim();
    if (stateOverride)
        return stateOverride;
    return path__namespace$1.join(os__namespace$1.homedir(), ".openclaw");
}
/**
 * 在 getDefaultMediaLocalRoots() 基础上，将 stateDir 本身也加入白名单，
 * 并合并用户在 WeComConfig 中配置的自定义 mediaLocalRoots。
 *
 * getDefaultMediaLocalRoots() 仅包含 stateDir 下的子目录（media/agents/workspace/sandboxes），
 * 但 agent 生成的文件可能直接放在 stateDir 根目录下（如 ~/.openclaw-dev/1.png），
 * 因此需要将 stateDir 本身也加入白名单以避免 LocalMediaAccessError。
 *
 * 用户可在 openclaw.json 中配置：
 * {
 *   "channels": {
 *     "wecom": {
 *       "mediaLocalRoots": ["~/Downloads", "~/Documents"]
 *     }
 *   }
 * }
 */
async function getExtendedMediaLocalRoots(config) {
    // 从兼容层获取默认白名单（内部已处理低版本 SDK 的 fallback）
    const defaults = await getDefaultMediaLocalRoots();
    const roots = [...defaults];
    const stateDir = path__namespace$1.resolve(resolveStateDir());
    if (!roots.includes(stateDir)) {
        roots.push(stateDir);
    }
    // 合并用户在 WeComConfig 中配置的自定义路径
    if (config?.mediaLocalRoots) {
        for (const r of config.mediaLocalRoots) {
            const resolved = path__namespace$1.resolve(r.replace(/^~(?=\/|$)/, os__namespace$1.homedir()));
            if (!roots.includes(resolved)) {
                roots.push(resolved);
            }
        }
    }
    return roots;
}
// ============================================================================
// 媒体发送错误提示
// ============================================================================
/**
 * 根据媒体发送结果生成纯文本错误摘要（用于替换 thinking 流式消息展示给用户）。
 *
 * 使用纯文本而非 markdown 格式，因为 replyStream 只支持纯文本。
 */
function buildMediaErrorSummary(mediaUrl, result) {
    if (result.error?.includes("LocalMediaAccessError")) {
        return `⚠️ 文件发送失败：没有权限访问路径 ${mediaUrl}\n请在 openclaw.json 的 mediaLocalRoots 中添加该路径的父目录后重启生效。`;
    }
    if (result.rejectReason) {
        return `⚠️ 文件发送失败：${result.rejectReason}`;
    }
    return `⚠️ 文件发送失败：无法处理文件 ${mediaUrl}，请稍后再试。`;
}
// ============================================================================
// 消息上下文构建
// ============================================================================
/**
 * 构建消息上下文
 */
function buildMessageContext(frame, account, config, text, mediaList, quoteContent) {
    const core = getWeComRuntime();
    const body = frame.body;
    const chatId = body.chatid || body.from.userid;
    const chatType = body.chattype === "group" ? "group" : "direct";
    // 解析路由信息
    const route = core.channel.routing.resolveAgentRoute({
        cfg: config,
        channel: CHANNEL_ID,
        accountId: account.accountId,
        peer: {
            kind: chatType,
            id: chatId,
        },
    });
    // 构建会话标签
    const fromLabel = chatType === "group" ? `group:${chatId}` : `user:${body.from.userid}`;
    // 当只有媒体没有文本时，使用占位符标识媒体类型
    const hasImages = mediaList.some((m) => m.contentType?.startsWith("image/"));
    const messageBody = text || (mediaList.length > 0 ? (hasImages ? MEDIA_IMAGE_PLACEHOLDER : MEDIA_DOCUMENT_PLACEHOLDER) : "");
    // 构建多媒体数组
    const mediaPaths = mediaList.length > 0 ? mediaList.map((m) => m.path) : undefined;
    const mediaTypes = mediaList.length > 0
        ? mediaList.map((m) => m.contentType).filter(Boolean)
        : undefined;
    // 构建标准消息上下文
    return core.channel.reply.finalizeInboundContext({
        Body: messageBody,
        RawBody: messageBody,
        CommandBody: messageBody,
        MessageSid: body.msgid,
        From: chatType === "group" ? `${CHANNEL_ID}:group:${chatId}` : `${CHANNEL_ID}:${body.from.userid}`,
        To: `${CHANNEL_ID}:${chatId}`,
        SenderId: body.from.userid,
        SessionKey: route.sessionKey,
        AccountId: account.accountId,
        ChatType: chatType,
        ConversationLabel: fromLabel,
        Timestamp: Date.now(),
        Provider: CHANNEL_ID,
        Surface: CHANNEL_ID,
        OriginatingChannel: CHANNEL_ID,
        OriginatingTo: `${CHANNEL_ID}:${chatId}`,
        CommandAuthorized: true,
        ResponseUrl: body.response_url,
        ReqId: frame.headers.req_id,
        WeComFrame: frame,
        MediaPath: mediaList[0]?.path,
        MediaType: mediaList[0]?.contentType,
        MediaPaths: mediaPaths,
        MediaTypes: mediaTypes,
        MediaUrls: mediaPaths,
        ReplyToBody: quoteContent,
    });
}
/**
 * 发送"思考中"消息
 */
async function sendThinkingReply(params) {
    const { wsClient, frame, streamId, runtime } = params;
    try {
        await sendWeComReply({
            wsClient,
            frame,
            text: THINKING_MESSAGE,
            runtime,
            finish: false,
            streamId,
        });
    }
    catch (err) {
        runtime.error?.(`[wecom] Failed to send thinking message: ${String(err)}`);
    }
}
/**
 * 累积文本并判断是否有可见内容（去除 <think> 标签后）
 */
function accumulateText(state, text) {
    state.accumulatedText += text;
    if (!state.hasText && stripThinkTags(state.accumulatedText)) {
        state.hasText = true;
    }
}
/**
 * 上传并发送一批媒体文件（统一走主动发送通道）
 *
 * replyMedia（被动回复）无法覆盖 replyStream 发出的 thinking 流式消息，
 * 因此所有媒体统一走 aibot_send_msg 主动发送。
 */
async function sendMediaBatch(ctx, mediaUrls) {
    const { wsClient, frame, state, account, runtime } = ctx;
    const body = frame.body;
    const chatId = body.chatid || body.from.userid;
    const mediaLocalRoots = await getExtendedMediaLocalRoots(account.config);
    runtime.log?.(`[wecom][debug] mediaLocalRoots=${JSON.stringify(mediaLocalRoots)}, mediaUrls=${JSON.stringify(mediaUrls)}, hasText=${!!state.hasText}`);
    for (const mediaUrl of mediaUrls) {
        const result = await uploadAndSendMedia({
            wsClient,
            mediaUrl,
            chatId,
            mediaLocalRoots,
            log: (...args) => runtime.log?.(...args),
            errorLog: (...args) => runtime.error?.(...args),
        });
        if (result.ok) {
            state.hasMedia = true;
        }
        else {
            state.hasMediaFailed = true;
            runtime.error?.(`[wecom] Media send failed: url=${mediaUrl}, reason=${result.rejectReason || result.error}`);
            // 收集错误摘要，后续在 finishThinkingStream 中直接替换 thinking 流展示给用户
            const summary = buildMediaErrorSummary(mediaUrl, result);
            state.mediaErrorSummary = state.mediaErrorSummary
                ? `${state.mediaErrorSummary}\n\n${summary}`
                : summary;
        }
    }
}
/**
 * 关闭 thinking 流（发送 finish=true 的流式消息）
 *
 * thinking 是通过 replyStream 用 streamId 发的流式消息，
 * 只有同一 streamId 的 replyStream(finish=true) 才能关闭它。
 *
 * ⚠️ 注意：企微会忽略空格等不可见内容，必须用有可见字符的文案才能真正
 *    替换掉 thinking 动画，否则 thinking 会一直残留。
 *
 * 关闭策略（按优先级）：
 * 1. 有可见文本 → 用完整文本关闭
 * 2. 有媒体成功发送（通过 deliver 回调） → 用友好提示"文件已发送"
 * 3. 媒体发送失败 → 直接用错误摘要替换 thinking
 * 4. 其他 → 用通用"处理完成"提示
 *    （agent 可能已通过内置 message 工具直接发送了文件，
 *    该路径走 outbound.sendMedia 完全绕过 deliver 回调，
 *    所以 state 中无记录，但文件已实际送达）
 */
async function finishThinkingStream(ctx) {
    const { wsClient, frame, state, runtime } = ctx;
    const visibleText = stripThinkTags(state.accumulatedText);
    let finishText;
    if (visibleText) {
        // 有可见文本：用完整文本关闭流（覆盖 thinking 为真实内容）
        finishText = state.accumulatedText;
    }
    else if (state.hasMedia) {
        // 媒体成功发送：用友好提示告知用户
        finishText = "📎 文件已发送，请查收。";
    }
    else if (state.hasMediaFailed && state.mediaErrorSummary) {
        // 媒体发送失败：直接用错误摘要替换 thinking 流（不再额外发 sendMessage）
        finishText = state.mediaErrorSummary;
    }
    else {
        // 核心无可见文本且 deliver 中未处理过媒体。
        //
        // 不使用错误提示，因为 agent 可能已通过内置 message 工具直接调用
        // outbound.sendMedia 成功发送了文件——该路径完全绕过 monitor 的 deliver
        // 回调，所以 state.deliverCalled / state.hasMedia 均为 false，但文件
        // 实际已送达用户。此时显示 "未生成回复" 会误导用户。
        finishText = "✅ 处理完成。";
    }
    await sendWeComReply({ wsClient, frame, text: finishText, runtime, finish: true, streamId: state.streamId });
}
/**
 * 路由消息到核心处理流程并处理回复
 */
async function routeAndDispatchMessage(params) {
    const { ctxPayload, config, account, wsClient, frame, state, runtime, onCleanup } = params;
    const core = getWeComRuntime();
    const ctx = { wsClient, frame, state, account, runtime };
    // 防止 onCleanup 被多次调用（onError 回调与 catch 块可能重复触发）
    let cleanedUp = false;
    const safeCleanup = () => {
        if (!cleanedUp) {
            cleanedUp = true;
            onCleanup();
        }
    };
    try {
        await core.channel.reply.dispatchReplyWithBufferedBlockDispatcher({
            ctx: ctxPayload,
            cfg: config,
            dispatcherOptions: {
                deliver: async (payload, info) => {
                    state.deliverCalled = true;
                    runtime.log?.(`[openclaw -> plugin] kind=${info.kind}, text=${payload.text ?? ''}, mediaUrl=${payload.mediaUrl ?? ''}, mediaUrls=${JSON.stringify(payload.mediaUrls ?? [])}`);
                    // 累积文本
                    if (payload.text) {
                        accumulateText(state, payload.text);
                    }
                    // 发送媒体（统一走主动发送）
                    const mediaUrls = payload.mediaUrls?.length ? payload.mediaUrls : payload.mediaUrl ? [payload.mediaUrl] : [];
                    if (mediaUrls.length > 0) {
                        try {
                            await sendMediaBatch(ctx, mediaUrls);
                        }
                        catch (mediaErr) {
                            // sendMediaBatch 内部异常（如 getDefaultMediaLocalRoots 不可用等）
                            // 必须标记 state，否则 finishThinkingStream 会显示"处理完成"误导用户
                            state.hasMediaFailed = true;
                            const errMsg = String(mediaErr);
                            const summary = `⚠️ 文件发送失败：内部处理异常，请升级 openclaw 到最新版本后重试。\n错误详情：${errMsg}`;
                            state.mediaErrorSummary = state.mediaErrorSummary
                                ? `${state.mediaErrorSummary}\n\n${summary}`
                                : summary;
                            runtime.error?.(`[wecom] sendMediaBatch threw: ${errMsg}`);
                        }
                    }
                    // 中间帧：有可见文本时流式更新
                    if (info.kind !== "final" && state.hasText && state.accumulatedText) {
                        await sendWeComReply({ wsClient, frame, text: state.accumulatedText, runtime, finish: false, streamId: state.streamId });
                    }
                },
                onError: (err, info) => {
                    runtime.error?.(`[wecom] ${info.kind} reply failed: ${String(err)}`);
                },
            },
        });
        // 关闭 thinking 流
        await finishThinkingStream(ctx);
        safeCleanup();
    }
    catch (err) {
        runtime.error?.(`[wecom][plugin] Failed to process message: ${String(err)}`);
        // 即使 dispatch 抛异常，也需要关闭 thinking 流，
        // 避免 deliver 已成功发送媒体但后续出错时 thinking 消息残留或被错误文案覆盖
        try {
            await finishThinkingStream(ctx);
        }
        catch (finishErr) {
            runtime.error?.(`[wecom] Failed to finish thinking stream after dispatch error: ${String(finishErr)}`);
        }
        safeCleanup();
    }
}
/**
 * 处理企业微信消息（主函数）
 *
 * 处理流程：
 * 1. 解析消息内容（文本、图片、引用）
 * 2. 群组策略检查（仅群聊）
 * 3. DM Policy 访问控制检查（仅私聊）
 * 4. 下载并保存图片
 * 5. 初始化消息状态
 * 6. 发送"思考中"消息
 * 7. 路由消息到核心处理流程
 *
 * 整体带超时保护，防止单条消息处理阻塞过久
 */
async function processWeComMessage(params) {
    const { frame, account, config, runtime, wsClient } = params;
    const body = frame.body;
    const chatId = body.chatid || body.from.userid;
    const chatType = body.chattype === "group" ? "group" : "direct";
    const messageId = body.msgid;
    const reqId = frame.headers.req_id;
    // Step 1: 解析消息内容
    const { textParts, imageUrls, imageAesKeys, fileUrls, fileAesKeys, quoteContent } = parseMessageContent(body);
    let text = textParts.join("\n").trim();
    // // 群聊中移除 @机器人 的提及标记
    // if (body.chattype === "group") {
    //   text = text.replace(/@\S+/g, "").trim();
    // }
    // 如果文本为空但存在引用消息，使用引用消息内容
    if (!text && quoteContent) {
        text = quoteContent;
        runtime.log?.("[wecom][plugin] Using quote content as message body (user only mentioned bot)");
    }
    // 如果既没有文本也没有图片也没有文件也没有引用内容，则跳过
    if (!text && imageUrls.length === 0 && fileUrls.length === 0) {
        runtime.log?.("[wecom][plugin] Skipping empty message (no text, image, file or quote)");
        return;
    }
    // Step 2: 群组策略检查（仅群聊）
    if (chatType === "group") {
        const groupPolicyResult = checkGroupPolicy({
            chatId,
            senderId: body.from.userid,
            account,
            config,
            runtime,
        });
        if (!groupPolicyResult.allowed) {
            return;
        }
    }
    // Step 3: DM Policy 访问控制检查（仅私聊）
    const dmPolicyResult = await checkDmPolicy({
        senderId: body.from.userid,
        isGroup: chatType === "group",
        account,
        wsClient,
        frame,
        runtime,
    });
    if (!dmPolicyResult.allowed) {
        return;
    }
    // Step 4: 下载并保存图片和文件
    const [imageMediaList, fileMediaList] = await Promise.all([
        downloadAndSaveImages({
            imageUrls,
            imageAesKeys,
            account,
            config,
            runtime,
            wsClient,
        }),
        downloadAndSaveFiles({
            fileUrls,
            fileAesKeys,
            account,
            config,
            runtime,
            wsClient,
        }),
    ]);
    const mediaList = [...imageMediaList, ...fileMediaList];
    // Step 5: 初始化消息状态
    setReqIdForChat(chatId, reqId, account.accountId);
    const streamId = aibotNodeSdk.generateReqId("stream");
    const state = { accumulatedText: "", streamId };
    setMessageState(messageId, state);
    const cleanupState = () => {
        deleteMessageState(messageId);
    };
    // Step 6: 发送"思考中"消息
    const shouldSendThinking = account.sendThinkingMessage ?? true;
    if (shouldSendThinking) {
        await sendThinkingReply({ wsClient, frame, streamId, runtime });
    }
    // Step 7: 构建上下文并路由到核心处理流程（带整体超时保护）
    const ctxPayload = buildMessageContext(frame, account, config, text, mediaList, quoteContent);
    runtime.log?.(`[plugin -> openclaw] body=${text}, mediaPaths=${JSON.stringify(mediaList.map(m => m.path))}${quoteContent ? `, quote=${quoteContent}` : ''}`);
    try {
        await withTimeout(routeAndDispatchMessage({
            ctxPayload,
            config,
            account,
            wsClient,
            frame,
            state,
            runtime,
            onCleanup: cleanupState,
        }), MESSAGE_PROCESS_TIMEOUT_MS, `Message processing timed out (msgId=${messageId})`);
    }
    catch (err) {
        runtime.error?.(`[wecom][plugin] Message processing failed or timed out: ${String(err)}`);
        // 确保 thinking 流被关闭，防止异常/超时时 thinking 消息一直残留
        try {
            if (shouldSendThinking) {
                await sendWeComReply({
                    wsClient,
                    frame,
                    text: "处理消息时出现异常，请稍后重试。",
                    runtime,
                    finish: true,
                    streamId: state.streamId,
                });
            }
        }
        catch (finishErr) {
            runtime.error?.(`[wecom] Failed to finish thinking stream on error: ${String(finishErr)}`);
        }
        cleanupState();
    }
}
// ============================================================================
// 创建 SDK Logger 适配器
// ============================================================================
/**
 * 创建适配 RuntimeEnv 的 Logger
 */
function createSdkLogger(runtime, accountId) {
    return {
        debug: (message, ...args) => {
            runtime.log?.(`[${accountId}] ${message}`, ...args);
        },
        info: (message, ...args) => {
            runtime.log?.(`[${accountId}] ${message}`, ...args);
        },
        warn: (message, ...args) => {
            runtime.log?.(`[${accountId}] WARN: ${message}`, ...args);
        },
        error: (message, ...args) => {
            runtime.error?.(`[${accountId}] ${message}`, ...args);
        },
    };
}
// ============================================================================
// 主函数
// ============================================================================
/**
 * 监听企业微信 WebSocket 连接
 * 使用 aibot-node-sdk 简化连接管理
 */
async function monitorWeComProvider(options) {
    const { account, config, runtime, abortSignal } = options;
    runtime.log?.(`[${account.accountId}] Initializing WSClient with SDK...`);
    // 启动消息状态定期清理
    startMessageStateCleanup();
    return new Promise((resolve, reject) => {
        const logger = createSdkLogger(runtime, account.accountId);
        const wsClient = new aibotNodeSdk.WSClient({
            botId: account.botId,
            secret: account.secret,
            wsUrl: account.websocketUrl,
            logger,
            heartbeatInterval: WS_HEARTBEAT_INTERVAL_MS,
            maxReconnectAttempts: WS_MAX_RECONNECT_ATTEMPTS,
        });
        // 清理函数：确保所有资源被释放
        const cleanup = async () => {
            stopMessageStateCleanup();
            await cleanupAccount(account.accountId);
        };
        // 处理中止信号
        if (abortSignal) {
            abortSignal.addEventListener("abort", async () => {
                runtime.log?.(`[${account.accountId}] Connection aborted`);
                await cleanup();
                resolve();
            });
        }
        // 监听连接事件
        wsClient.on("connected", () => {
            runtime.log?.(`[${account.accountId}] WebSocket connected`);
        });
        // 监听认证成功事件
        wsClient.on("authenticated", () => {
            runtime.log?.(`[${account.accountId}] Authentication successful`);
            setWeComWebSocket(account.accountId, wsClient);
            // 认证成功后自动拉取 MCP 配置（异步，失败不影响主流程）
            fetchAndSaveMcpConfig(wsClient, account.accountId, runtime);
        });
        // 监听断开事件
        wsClient.on("disconnected", (reason) => {
            runtime.log?.(`[${account.accountId}] WebSocket disconnected: ${reason}`);
        });
        // 监听重连事件
        wsClient.on("reconnecting", (attempt) => {
            runtime.log?.(`[${account.accountId}] Reconnecting attempt ${attempt}...`);
        });
        // 监听错误事件
        wsClient.on("error", (error) => {
            runtime.error?.(`[${account.accountId}] WebSocket error: ${error.message}`);
            // 认证失败时拒绝 Promise
            if (error.message.includes("Authentication failed")) {
                cleanup().finally(() => reject(error));
            }
        });
        // 监听所有消息
        wsClient.on("message", async (frame) => {
            try {
                await processWeComMessage({
                    frame,
                    account,
                    config,
                    runtime,
                    wsClient,
                });
            }
            catch (err) {
                runtime.error?.(`[${account.accountId}] Failed to process message: ${String(err)}`);
            }
        });
        // 启动前预热 reqId 缓存，确保完成后再建立连接，避免 getSync 在预热完成前返回 undefined
        warmupReqIdStore(account.accountId, (...args) => runtime.log?.(...args))
            .then((count) => {
            runtime.log?.(`[${account.accountId}] Warmed up ${count} reqId entries from disk`);
        })
            .catch((err) => {
            runtime.error?.(`[${account.accountId}] Failed to warmup reqId store: ${String(err)}`);
        })
            .finally(() => {
            // 无论预热成功或失败，都建立连接
            wsClient.connect();
        });
    });
}

/**
 * 企业微信公共工具函数
 */
const DefaultWsUrl = "wss://openws.work.weixin.qq.com";
/**
 * 解析企业微信账户配置
 */
function resolveWeComAccount(cfg) {
    const wecomConfig = (cfg.channels?.[CHANNEL_ID] ?? {});
    return {
        accountId: pluginSdk.DEFAULT_ACCOUNT_ID,
        name: wecomConfig.name ?? "企业微信",
        enabled: wecomConfig.enabled ?? false,
        websocketUrl: wecomConfig.websocketUrl || DefaultWsUrl,
        botId: wecomConfig.botId ?? "",
        secret: wecomConfig.secret ?? "",
        sendThinkingMessage: wecomConfig.sendThinkingMessage ?? true,
        config: wecomConfig,
    };
}
/**
 * 设置企业微信账户配置
 */
function setWeComAccount(cfg, account) {
    const existing = (cfg.channels?.[CHANNEL_ID] ?? {});
    const merged = {
        enabled: account.enabled ?? existing?.enabled ?? true,
        botId: account.botId ?? existing?.botId ?? "",
        secret: account.secret ?? existing?.secret ?? "",
        allowFrom: account.allowFrom ?? existing?.allowFrom,
        dmPolicy: account.dmPolicy ?? existing?.dmPolicy,
        // 以下字段仅在已有配置值或显式传入时才写入，onboarding 时不主动生成
        ...(account.websocketUrl || existing?.websocketUrl
            ? { websocketUrl: account.websocketUrl ?? existing?.websocketUrl }
            : {}),
        ...(account.name || existing?.name
            ? { name: account.name ?? existing?.name }
            : {}),
        ...(account.sendThinkingMessage !== undefined || existing?.sendThinkingMessage !== undefined
            ? { sendThinkingMessage: account.sendThinkingMessage ?? existing?.sendThinkingMessage }
            : {}),
    };
    return {
        ...cfg,
        channels: {
            ...cfg.channels,
            [CHANNEL_ID]: merged,
        },
    };
}

/**
 * 企业微信 onboarding adapter for CLI setup wizard.
 */
const channel = CHANNEL_ID;
/**
 * 企业微信设置帮助说明
 */
async function noteWeComSetupHelp(prompter) {
    await prompter.note([
        "企业微信机器人需要以下配置信息：",
        "1. Bot ID: 企业微信机器人id",
        "2. Secret: 企业微信机器人密钥",
    ].join("\n"), "企业微信设置");
}
/**
 * 提示输入 Bot ID
 */
async function promptBotId(prompter, account) {
    return String(await prompter.text({
        message: "企业微信机器人 Bot ID",
        initialValue: account?.botId ?? "",
        validate: (value) => (value?.trim() ? undefined : "Required"),
    })).trim();
}
/**
 * 提示输入 Secret
 */
async function promptSecret(prompter, account) {
    return String(await prompter.text({
        message: "企业微信机器人 Secret",
        initialValue: account?.secret ?? "",
        validate: (value) => (value?.trim() ? undefined : "Required"),
    })).trim();
}
/**
 * 设置企业微信 dmPolicy
 */
function setWeComDmPolicy(cfg, dmPolicy) {
    const account = resolveWeComAccount(cfg);
    const existingAllowFrom = account.config.allowFrom ?? [];
    const allowFrom = dmPolicy === "open"
        ? pluginSdk.addWildcardAllowFrom(existingAllowFrom.map((x) => String(x)))
        : existingAllowFrom.map((x) => String(x));
    return setWeComAccount(cfg, {
        dmPolicy,
        allowFrom,
    });
}
const dmPolicy = {
    label: "企业微信",
    channel,
    policyKey: `channels.${CHANNEL_ID}.dmPolicy`,
    allowFromKey: `channels.${CHANNEL_ID}.allowFrom`,
    getCurrent: (cfg) => {
        const account = resolveWeComAccount(cfg);
        return account.config.dmPolicy ?? "open";
    },
    setPolicy: (cfg, policy) => {
        return setWeComDmPolicy(cfg, policy);
    },
    promptAllowFrom: async ({ cfg, prompter }) => {
        const account = resolveWeComAccount(cfg);
        const existingAllowFrom = account.config.allowFrom ?? [];
        const entry = await prompter.text({
            message: "企业微信允许来源（用户ID或群组ID，每行一个，推荐用于安全控制）",
            placeholder: "user123 或 group456",
            initialValue: existingAllowFrom[0] ? String(existingAllowFrom[0]) : undefined,
        });
        const allowFrom = String(entry ?? "")
            .split(/[\n,;]+/g)
            .map((s) => s.trim())
            .filter(Boolean);
        return setWeComAccount(cfg, { allowFrom });
    },
};
const wecomOnboardingAdapter = {
    channel,
    getStatus: async ({ cfg }) => {
        const account = resolveWeComAccount(cfg);
        const configured = Boolean(account.botId?.trim() &&
            account.secret?.trim());
        return {
            channel,
            configured,
            statusLines: [`企业微信: ${configured ? "已配置" : "需要 Bot ID 和 Secret"}`],
            selectionHint: configured ? "已配置" : "需要设置",
        };
    },
    configure: async ({ cfg, prompter, forceAllowFrom }) => {
        const account = resolveWeComAccount(cfg);
        if (!account.botId?.trim() || !account.secret?.trim()) {
            await noteWeComSetupHelp(prompter);
        }
        // 提示输入必要的配置信息：Bot ID 和 Secret
        const botId = await promptBotId(prompter, account);
        const secret = await promptSecret(prompter, account);
        // 使用默认值配置其他选项
        const cfgWithAccount = setWeComAccount(cfg, {
            botId,
            secret,
            enabled: true,
            dmPolicy: account.config.dmPolicy ?? "open",
            allowFrom: account.config.allowFrom ?? [],
        });
        return { cfg: cfgWithAccount };
    },
    dmPolicy,
    disable: (cfg) => {
        return setWeComAccount(cfg, { enabled: false });
    },
};

/**
 * 使用 SDK 的 sendMessage 主动发送企业微信消息
 * 无需依赖 reqId，直接向指定会话推送消息
 */
async function sendWeComMessage({ to, content, accountId, }) {
    const resolvedAccountId = accountId ?? pluginSdk.DEFAULT_ACCOUNT_ID;
    // 从 to 中提取 chatId（格式是 "${CHANNEL_ID}:chatId" 或直接是 chatId）
    const channelPrefix = new RegExp(`^${CHANNEL_ID}:`, "i");
    const chatId = to.replace(channelPrefix, "");
    // 获取 WSClient 实例
    const wsClient = getWeComWebSocket(resolvedAccountId);
    if (!wsClient) {
        throw new Error(`WSClient not connected for account ${resolvedAccountId}`);
    }
    // 使用 SDK 的 sendMessage 主动发送 markdown 消息
    const result = await wsClient.sendMessage(chatId, {
        msgtype: 'markdown',
        markdown: { content },
    });
    const messageId = result?.headers?.req_id ?? `wecom-${Date.now()}`;
    return {
        channel: CHANNEL_ID,
        messageId,
        chatId,
    };
}
// 企业微信频道元数据
const meta = {
    id: CHANNEL_ID,
    label: "企业微信",
    selectionLabel: "企业微信 (WeCom)",
    detailLabel: "企业微信智能机器人",
    docsPath: `/channels/${CHANNEL_ID}`,
    docsLabel: CHANNEL_ID,
    blurb: "企业微信智能机器人接入插件",
    systemImage: "message.fill",
};
const wecomPlugin = {
    id: CHANNEL_ID,
    meta: {
        ...meta,
        quickstartAllowFrom: true,
    },
    pairing: {
        idLabel: "wecomUserId",
        normalizeAllowEntry: (entry) => entry.replace(new RegExp(`^(${CHANNEL_ID}|user):`, "i"), "").trim(),
        notifyApproval: async ({ cfg, id }) => {
            // sendWeComMessage({
            //   to: id,
            //   content: " pairing approved",
            //   accountId: cfg.accountId,
            // });
            // Pairing approved for user
        },
    },
    onboarding: wecomOnboardingAdapter,
    capabilities: {
        chatTypes: ["direct", "group"],
        reactions: false,
        threads: false,
        media: true,
        nativeCommands: false,
        blockStreaming: true,
    },
    reload: { configPrefixes: [`channels.${CHANNEL_ID}`] },
    config: {
        // 列出所有账户 ID（最小实现只支持默认账户）
        listAccountIds: () => [pluginSdk.DEFAULT_ACCOUNT_ID],
        // 解析账户配置
        resolveAccount: (cfg) => resolveWeComAccount(cfg),
        // 获取默认账户 ID
        defaultAccountId: () => pluginSdk.DEFAULT_ACCOUNT_ID,
        // 设置账户启用状态
        setAccountEnabled: ({ cfg, enabled }) => {
            const wecomConfig = (cfg.channels?.[CHANNEL_ID] ?? {});
            return {
                ...cfg,
                channels: {
                    ...cfg.channels,
                    [CHANNEL_ID]: {
                        ...wecomConfig,
                        enabled,
                    },
                },
            };
        },
        // 删除账户
        deleteAccount: ({ cfg }) => {
            const wecomConfig = (cfg.channels?.[CHANNEL_ID] ?? {});
            const { botId, secret, ...rest } = wecomConfig;
            return {
                ...cfg,
                channels: {
                    ...cfg.channels,
                    [CHANNEL_ID]: rest,
                },
            };
        },
        // 检查是否已配置
        isConfigured: (account) => Boolean(account.botId?.trim() && account.secret?.trim()),
        // 描述账户信息
        describeAccount: (account) => ({
            accountId: account.accountId,
            name: account.name,
            enabled: account.enabled,
            configured: Boolean(account.botId?.trim() && account.secret?.trim()),
            botId: account.botId,
            websocketUrl: account.websocketUrl,
        }),
        // 解析允许来源列表
        resolveAllowFrom: ({ cfg }) => {
            const account = resolveWeComAccount(cfg);
            return (account.config.allowFrom ?? []).map((entry) => String(entry));
        },
        // 格式化允许来源列表
        formatAllowFrom: ({ allowFrom }) => allowFrom
            .map((entry) => String(entry).trim())
            .filter(Boolean),
    },
    security: {
        resolveDmPolicy: ({ account }) => {
            const basePath = `channels.${CHANNEL_ID}.`;
            return {
                policy: account.config.dmPolicy ?? "open",
                allowFrom: account.config.allowFrom ?? [],
                policyPath: `${basePath}dmPolicy`,
                allowFromPath: basePath,
                approveHint: pluginSdk.formatPairingApproveHint(CHANNEL_ID),
                normalizeEntry: (raw) => raw.replace(new RegExp(`^${CHANNEL_ID}:`, "i"), "").trim(),
            };
        },
        collectWarnings: ({ account, cfg }) => {
            const warnings = [];
            // DM 策略警告
            const dmPolicy = account.config.dmPolicy ?? "open";
            if (dmPolicy === "open") {
                const hasWildcard = (account.config.allowFrom ?? []).some((entry) => String(entry).trim() === "*");
                if (!hasWildcard) {
                    warnings.push(`- 企业微信私信：dmPolicy="open" 但 allowFrom 未包含 "*"。任何人都可以发消息，但允许列表为空可能导致意外行为。建议设置 channels.${CHANNEL_ID}.allowFrom=["*"] 或使用 dmPolicy="pairing"。`);
                }
            }
            // 群组策略警告
            const defaultGroupPolicy = cfg.channels?.defaults?.groupPolicy;
            const groupPolicy = account.config.groupPolicy ?? defaultGroupPolicy ?? "open";
            // const { groupPolicy } = resolveOpenProviderRuntimeGroupPolicy({
            //   providerConfigPresent: true,
            //   groupPolicy: account.config.groupPolicy,
            //   defaultGroupPolicy,
            // });
            if (groupPolicy === "open") {
                warnings.push(`- 企业微信群组：groupPolicy="open" 允许所有群组中的成员触发。设置 channels.${CHANNEL_ID}.groupPolicy="allowlist" + channels.${CHANNEL_ID}.groupAllowFrom 来限制群组。`);
            }
            return warnings;
        },
    },
    messaging: {
        normalizeTarget: (target) => {
            const trimmed = target.trim();
            if (!trimmed)
                return undefined;
            return trimmed;
        },
        targetResolver: {
            looksLikeId: (id) => {
                const trimmed = id?.trim();
                return Boolean(trimmed);
            },
            hint: "<userId|groupId>",
        },
    },
    directory: {
        self: async () => null,
        listPeers: async () => [],
        listGroups: async () => [],
    },
    outbound: {
        deliveryMode: "gateway",
        chunker: (text, limit) => getWeComRuntime().channel.text.chunkMarkdownText(text, limit),
        textChunkLimit: TEXT_CHUNK_LIMIT,
        sendText: async ({ to, text, accountId }) => {
            return sendWeComMessage({ to, content: text, accountId: accountId ?? undefined });
        },
        sendMedia: async ({ to, text, mediaUrl, mediaLocalRoots, accountId }) => {
            const resolvedAccountId = accountId ?? pluginSdk.DEFAULT_ACCOUNT_ID;
            const channelPrefix = new RegExp(`^${CHANNEL_ID}:`, "i");
            const chatId = to.replace(channelPrefix, "");
            // 获取 WSClient 实例
            const wsClient = getWeComWebSocket(resolvedAccountId);
            if (!wsClient) {
                throw new Error(`WSClient not connected for account ${resolvedAccountId}`);
            }
            // 如果没有 mediaUrl，fallback 为纯文本
            if (!mediaUrl) {
                return sendWeComMessage({ to, content: text || "", accountId: resolvedAccountId });
            }
            const result = await uploadAndSendMedia({
                wsClient,
                mediaUrl,
                chatId,
                mediaLocalRoots,
            });
            if (result.rejected) {
                return sendWeComMessage({ to, content: `⚠️ ${result.rejectReason}`, accountId: resolvedAccountId });
            }
            if (!result.ok) {
                // 上传/发送失败，fallback 为文本 + URL
                const fallbackContent = text
                    ? `${text}\n📎 ${mediaUrl}`
                    : `📎 ${mediaUrl}`;
                return sendWeComMessage({ to, content: fallbackContent, accountId: resolvedAccountId });
            }
            // 如有伴随文本，额外发送一条 markdown
            if (text) {
                await sendWeComMessage({ to, content: text, accountId: resolvedAccountId });
            }
            // 如果有降级说明，额外发送提示
            if (result.downgradeNote) {
                await sendWeComMessage({ to, content: `ℹ️ ${result.downgradeNote}`, accountId: resolvedAccountId });
            }
            return {
                channel: CHANNEL_ID,
                messageId: result.messageId,
                chatId,
            };
        },
    },
    status: {
        defaultRuntime: {
            accountId: pluginSdk.DEFAULT_ACCOUNT_ID,
            running: false,
            lastStartAt: null,
            lastStopAt: null,
            lastError: null,
        },
        collectStatusIssues: (accounts) => accounts.flatMap((entry) => {
            const accountId = String(entry.accountId ?? pluginSdk.DEFAULT_ACCOUNT_ID);
            const enabled = entry.enabled !== false;
            const configured = entry.configured === true;
            if (!enabled) {
                return [];
            }
            const issues = [];
            if (!configured) {
                issues.push({
                    channel: CHANNEL_ID,
                    accountId,
                    kind: "config",
                    message: "企业微信机器人 ID 或 Secret 未配置",
                    fix: "Run: openclaw channels add wecom --bot-id <id> --secret <secret>",
                });
            }
            return issues;
        }),
        buildChannelSummary: ({ snapshot }) => ({
            configured: snapshot.configured ?? false,
            running: snapshot.running ?? false,
            lastStartAt: snapshot.lastStartAt ?? null,
            lastStopAt: snapshot.lastStopAt ?? null,
            lastError: snapshot.lastError ?? null,
        }),
        probeAccount: async () => {
            return { ok: true, status: 200 };
        },
        buildAccountSnapshot: ({ account, runtime }) => {
            const configured = Boolean(account.botId?.trim() &&
                account.secret?.trim());
            return {
                accountId: account.accountId,
                name: account.name,
                enabled: account.enabled,
                configured,
                running: runtime?.running ?? false,
                lastStartAt: runtime?.lastStartAt ?? null,
                lastStopAt: runtime?.lastStopAt ?? null,
                lastError: runtime?.lastError ?? null,
            };
        },
    },
    gateway: {
        startAccount: async (ctx) => {
            const account = ctx.account;
            // 启动 WebSocket 监听
            return monitorWeComProvider({
                account,
                config: ctx.cfg,
                runtime: ctx.runtime,
                abortSignal: ctx.abortSignal,
            });
        },
        logoutAccount: async ({ cfg }) => {
            const nextCfg = { ...cfg };
            const wecomConfig = (cfg.channels?.[CHANNEL_ID] ?? {});
            const nextWecom = { ...wecomConfig };
            let cleared = false;
            let changed = false;
            if (nextWecom.botId || nextWecom.secret) {
                delete nextWecom.botId;
                delete nextWecom.secret;
                cleared = true;
                changed = true;
            }
            if (changed) {
                if (Object.keys(nextWecom).length > 0) {
                    nextCfg.channels = { ...nextCfg.channels, [CHANNEL_ID]: nextWecom };
                }
                else {
                    const nextChannels = { ...nextCfg.channels };
                    delete nextChannels[CHANNEL_ID];
                    if (Object.keys(nextChannels).length > 0) {
                        nextCfg.channels = nextChannels;
                    }
                    else {
                        delete nextCfg.channels;
                    }
                }
                await getWeComRuntime().config.writeConfigFile(nextCfg);
            }
            const resolved = resolveWeComAccount(changed ? nextCfg : cfg);
            const loggedOut = !resolved.botId && !resolved.secret;
            return { cleared, envToken: false, loggedOut };
        },
    },
};

const plugin = {
    id: "wecom",
    name: "企业微信",
    description: "企业微信 OpenClaw 插件",
    configSchema: pluginSdk.emptyPluginConfigSchema(),
    register(api) {
        setWeComRuntime(api.runtime);
        api.registerChannel({ plugin: wecomPlugin });
        // 注入媒体发送指令和文件大小限制提示词
        api.on("before_prompt_build", () => {
            return {
                appendSystemContext: [
                    "【发送文件/图片/视频/语音】",
                    "当你需要向用户发送文件、图片、视频或语音时，必须在回复中单独一行使用 MEDIA: 指令，后面跟文件的本地路径。",
                    "格式：MEDIA: /文件的绝对路径",
                    "文件优先存放到 ~/.openclaw 目录下，确保路径可访问。",
                    "示例：",
                    "  MEDIA: ~/.openclaw/output.png",
                    "  MEDIA: ~/.openclaw/report.pdf",
                    "系统会自动识别文件类型并发送给用户。",
                    "",
                    "注意事项：",
                    "- MEDIA: 必须在行首，后面紧跟文件路径（不是 URL）",
                    "- 如果路径中包含空格，可以用反引号包裹：MEDIA: `/path/to/my file.png`",
                    "- 每个文件单独一行 MEDIA: 指令",
                    "- 可以在 MEDIA: 指令前后附带文字说明",
                    "",
                    "【文件大小限制】",
                    "- 图片不超过 10MB，视频不超过 10MB，语音不超过 2MB（仅支持 AMR 格式），文件不超过 20MB",
                    "- 语音消息仅支持 AMR 格式（.amr），如需发送语音请确保文件为 AMR 格式",
                    "- 超过大小限制的图片/视频/语音会被自动转为文件格式发送",
                    "- 如果文件超过 20MB，将无法发送，请提前告知用户并尝试缩减文件大小",
                ].join("\n"),
            };
        });
    },
};

exports.default = plugin;
//# sourceMappingURL=index.cjs.js.map
