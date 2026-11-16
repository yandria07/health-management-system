// 内容推荐 —— 内置静态内容库
// 说明：
//   1. 推荐逻辑是「需求标签 → 内容标签」匹配，本文件即内容侧的数据源。
//   2. 所有链接均可替换为你自己的合规内容。
//   3. tags 必须与 recommendEngine.js 中的需求 key 保持一致：
//      weight_high / weight_low / sleep_low / exercise_low / relax / mood_low / stress_high / anxiety_high
//   4. type 与字段：
//      'video'   → 整卡点击「页面跳转」，url 指向目标页（见下方 biliSearch）
//      'music'   → 整卡点击「页面跳转」，url 指向详情/播放页
//      'article' → 整卡点击「页面跳转」，url 指向文章，excerpt 为摘要
//   5. 视频链接说明：为避免「视频不存在」，这里统一指向 B站「关键词搜索结果页」（永远有真实可看视频）。
//      若你想固定到某条具体视频，把对应 url 改成 https://www.bilibili.com/video/BVxxxx 即可。

// —— 视频链接：指向 B站 搜索结果页（按主题关键词），保证跳转永远有真实可看视频 ——
const biliSearch = (kw) =>
  `https://search.bilibili.com/all?keyword=${encodeURIComponent(kw)}`

export const CONTENT_LIBRARY = [
  // ============ 体重偏高 weight_high ============
  {
    id: 'v-fit-20min',
    type: 'video',
    title: '20分钟全身燃脂操（零器械·居家）',
    tags: ['weight_high'],
    desc: '适合新手跟练，无需器械，持续 20 分钟提升心率、消耗热量。',
    url: biliSearch('居家燃脂操 无器械'),
    priority: 3,
  },
  {
    id: 'v-fatloss-plan',
    type: 'video',
    title: '新手友好减脂训练计划讲解',
    tags: ['weight_high'],
    desc: '讲清楚减脂底层逻辑：热量缺口、力量+有氧如何搭配。',
    url: biliSearch('新手减脂训练计划'),
    priority: 2,
  },
  {
    id: 'a-fatloss-science',
    type: 'article',
    title: '科学减脂：饮食与运动的平衡',
    tags: ['weight_high'],
    desc: '权威机构关于健康减重的核心建议，避免极端节食。',
    url: 'https://www.cdc.gov/healthyweight/index.html',
    excerpt:
      '减脂的本质是可持续的热量缺口。相比极端节食，规律运动+均衡膳食更能长期维持体重。',
    priority: 2,
  },
  {
    id: 'v-hiit-home',
    type: 'video',
    title: '居家 HIIT 燃脂（10 分钟）',
    tags: ['weight_high', 'exercise_low'],
    desc: '短时间高强度间歇，适合没时间去健身房的上班族。',
    url: biliSearch('居家HIIT燃脂 10分钟'),
    priority: 1,
  },

  // ============ 体重偏轻 weight_low ============
  {
    id: 'v-gain-train',
    type: 'video',
    title: '健康增重：力量训练入门',
    tags: ['weight_low'],
    desc: '通过抗阻训练增加肌肉量，而非单纯囤积脂肪。',
    url: biliSearch('健康增重 力量训练入门'),
    priority: 2,
  },
  {
    id: 'a-gain-nutrition',
    type: 'article',
    title: '偏瘦人群营养补充指南',
    tags: ['weight_low'],
    desc: '如何在保证消化负担可控的前提下提高每日热量摄入。',
    url: 'https://www.cma.org.cn/',
    excerpt:
      '增重同样需要「计划」：足量优质蛋白、规律加餐、配合力量训练，比盲目多吃更有效。',
    priority: 1,
  },

  // ============ 睡眠不足 sleep_low ============
  {
    id: 'm-rain',
    type: 'music',
    title: '深度睡眠 · 雨声白噪音',
    tags: ['sleep_low'],
    desc: '持续雨声掩蔽环境噪音，帮助更快进入睡眠。',
    url: 'https://soundbox.fun/',
    priority: 3,
  },
  {
    id: 'm-lullaby',
    type: 'music',
    title: '助眠轻音乐 · 舒缓钢琴',
    tags: ['sleep_low', 'relax'],
    desc: '慢节奏钢琴曲，降低交感神经兴奋，适合睡前聆听。',
    url: 'https://www.lacuna.fm/ai-radio-station',
    priority: 2,
  },
  {
    id: 'v-sleep-guide',
    type: 'video',
    title: '10 分钟睡前身体放松引导',
    tags: ['sleep_low'],
    desc: '渐进式肌肉放松 + 呼吸引导，缓解入睡困难。',
    url: biliSearch('睡前放松 身体扫描引导'),
    priority: 2,
  },
  {
    id: 'a-sleep-habits',
    type: 'article',
    title: '改善睡眠的 8 个日常习惯',
    tags: ['sleep_low'],
    desc: '从光照、咖啡因、睡前仪式入手，系统性提升睡眠质量。',
    url: 'https://www.sleepfoundation.org/sleep-hygiene',
    excerpt:
      '固定作息、睡前 1 小时远离屏幕、白天适度日晒，是被反复验证的有效睡眠卫生方法。',
    priority: 1,
  },

  // ============ 运动不足 exercise_low ============
  {
    id: 'v-home-move',
    type: 'video',
    title: '每天 15 分钟居家轻运动',
    tags: ['exercise_low'],
    desc: '无需器械、不累人，帮助久坐人群逐步恢复活动量。',
    url: biliSearch('居家轻运动 15分钟'),
    priority: 2,
  },
  {
    id: 'v-walk-fatburn',
    type: 'video',
    title: '散步也能燃脂：正确走法',
    tags: ['exercise_low', 'weight_high'],
    desc: '把日常步行变成有效有氧，轻松积累活动量。',
    url: biliSearch('快走燃脂 正确走法'),
    priority: 1,
  },

  // ============ 需放松减压 relax ============
  {
    id: 'v-breath-relax',
    type: 'video',
    title: '5 分钟呼吸放松训练',
    tags: ['relax', 'stress_high'],
    desc: '腹式呼吸 + 4-7-8 呼吸法，快速平复紧张。',
    url: biliSearch('呼吸放松训练 4-7-8'),
    priority: 2,
  },
  {
    id: 'm-afternoon',
    type: 'music',
    title: '午后解压轻音乐',
    tags: ['relax', 'mood_low'],
    desc: '柔和旋律，适合工作间隙放松神经。',
    url: 'https://soundbox.fun/',
    priority: 1,
  },

  // ============ 情绪压力 mood_low ============
  {
    id: 'v-mood-rescue',
    type: 'video',
    title: '走出情绪低谷 · 心理疏导',
    tags: ['mood_low'],
    desc: '从认知角度理解低落情绪，给出可操作的自助步骤。',
    url: biliSearch('心理疏导 走出情绪低谷'),
    priority: 3,
  },
  {
    id: 'v-mindfulness',
    type: 'video',
    title: '正念冥想 · 释放焦虑',
    tags: ['mood_low', 'anxiety_high'],
    desc: '跟随引导进行身体扫描与呼吸锚定，缓解反复思虑。',
    url: biliSearch('正念冥想 缓解焦虑'),
    priority: 2,
  },
  {
    id: 'a-lowmood-help',
    type: 'article',
    title: '当情绪持续低落，你可以这样做',
    tags: ['mood_low'],
    desc: '识别抑郁信号，了解何时需要寻求专业帮助。',
    url: 'https://www.mayoclinic.org/diseases-conditions/depression/symptoms-causes/syc-20356007',
    excerpt:
      '如果低落情绪持续两周以上并影响生活，请尽早联系专业心理或精神科医生，求助是强者的选择。',
    priority: 1,
  },
  {
    id: 'm-heal',
    type: 'music',
    title: '治愈系轻音乐合集',
    tags: ['mood_low', 'sleep_low'],
    desc: '温暖舒缓的旋律，给情绪一个被照顾的空间。',
    url: 'https://soundbox.fun/',
    priority: 1,
  },

  // ============ 压力较大 stress_high ============
  {
    id: 'v-work-stress',
    type: 'video',
    title: '职场减压 · 正念练习',
    tags: ['stress_high'],
    desc: '把正念融入工作间隙，降低慢性压力水平。',
    url: biliSearch('职场减压 正念练习'),
    priority: 2,
  },
  {
    id: 'a-stress-signals',
    type: 'article',
    title: '识别压力信号与应对方法',
    tags: ['stress_high'],
    desc: '了解身体与情绪的压力预警，建立自己的减压清单。',
    url: 'https://www.mayoclinic.org/healthy-lifestyle/stress-management/basics/stress-basics/hlv-20049495',
    excerpt:
      '压力并非全无意义，但长期高压会损害身心。规律的睡眠、运动与社交支持是最基础的抗压资源。',
    priority: 1,
  },

  // ============ 焦虑明显 anxiety_high ============
  {
    id: 'v-anxiety-breath',
    type: 'video',
    title: '缓解焦虑的呼吸法',
    tags: ['anxiety_high', 'relax'],
    desc: '急性期焦虑时可用，帮助把注意力拉回当下。',
    url: biliSearch('缓解焦虑 呼吸法'),
    priority: 2,
  },
  {
    id: 'a-anxiety-selfcare',
    type: 'article',
    title: '焦虑情绪的自我调适',
    tags: ['anxiety_high'],
    desc: '区分正常焦虑与焦虑障碍，掌握自助调节技巧。',
    url: 'https://www.mayoclinic.org/diseases-conditions/generalized-anxiety-disorder/symptoms-causes/syc-20360803',
    excerpt:
      '当担忧难以控制并持续超过六个月、影响生活时，建议咨询专业人士进行系统评估。',
    priority: 1,
  },
]
