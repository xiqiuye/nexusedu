/** 固定模拟数据，不请求业务接口。成绩按年级统计，AI 按日统计。 */
export const examLabels = ['9月摸底', '9月月考', '10月周测', '10月月考', '期中考试']
export const scoreBands = ['60分以下', '60–69分', '70–79分', '80–89分', '90–100分']

export const grades = [
  {
    name: '七年级',
    students: 420,
    trend: [72.1, 74.2, 75.8, 77.1, 79.4],
    distribution: [21, 42, 105, 168, 84],
    subjects: [
      { name: '语文', average: 81.6, previous: 79.2, pass: 96.2, excellent: 28.6 },
      { name: '数学', average: 78.2, previous: 75.4, pass: 93.1, excellent: 22.4 },
      { name: '英语', average: 83.4, previous: 80.6, pass: 97.4, excellent: 36.2 },
      { name: '科学', average: 76.8, previous: 73.2, pass: 93.3, excellent: 19.8 },
      { name: '道法', average: 77.0, previous: 77.1, pass: 95.0, excellent: 20.5 }
    ]
  },
  {
    name: '八年级',
    students: 400,
    trend: [70.6, 72.8, 74.1, 75.6, 77.8],
    distribution: [28, 52, 112, 140, 68],
    subjects: [
      { name: '语文', average: 80.2, previous: 78.4, pass: 94.5, excellent: 25.0 },
      { name: '数学', average: 75.4, previous: 72.8, pass: 89.5, excellent: 18.5 },
      { name: '英语', average: 81.6, previous: 79.2, pass: 95.5, excellent: 31.0 },
      { name: '科学', average: 74.8, previous: 71.6, pass: 90.0, excellent: 17.5 },
      { name: '道法', average: 77.0, previous: 76.0, pass: 95.5, excellent: 22.0 }
    ]
  },
  {
    name: '九年级',
    students: 380,
    trend: [71.4, 73.6, 75.2, 77.3, 80.2],
    distribution: [15, 34, 95, 152, 84],
    subjects: [
      { name: '语文', average: 82.4, previous: 79.8, pass: 96.8, excellent: 30.0 },
      { name: '数学', average: 79.6, previous: 76.2, pass: 94.2, excellent: 25.8 },
      { name: '英语', average: 84.0, previous: 81.4, pass: 97.9, excellent: 38.2 },
      { name: '科学', average: 77.2, previous: 74.3, pass: 93.7, excellent: 21.1 },
      { name: '道法', average: 77.8, previous: 74.8, pass: 97.6, excellent: 25.0 }
    ]
  }
]

export const aiDaily = [
  {
    date: '10/26',
    student: 86,
    knowledge: 132,
    learning: 105,
    other: 37,
    failed: 8,
    sessions: 112,
    users: 78,
    firstToken: 1.8,
    tokens: 432000
  },
  {
    date: '10/27',
    student: 94,
    knowledge: 146,
    learning: 118,
    other: 42,
    failed: 7,
    sessions: 126,
    users: 86,
    firstToken: 1.7,
    tokens: 480000
  },
  {
    date: '10/28',
    student: 102,
    knowledge: 155,
    learning: 126,
    other: 47,
    failed: 9,
    sessions: 138,
    users: 94,
    firstToken: 1.9,
    tokens: 516000
  },
  {
    date: '10/29',
    student: 98,
    knowledge: 168,
    learning: 136,
    other: 48,
    failed: 6,
    sessions: 145,
    users: 98,
    firstToken: 1.6,
    tokens: 540000
  },
  {
    date: '10/30',
    student: 116,
    knowledge: 179,
    learning: 148,
    other: 47,
    failed: 8,
    sessions: 158,
    users: 108,
    firstToken: 1.6,
    tokens: 588000
  },
  {
    date: '10/31',
    student: 64,
    knowledge: 121,
    learning: 112,
    other: 33,
    failed: 5,
    sessions: 108,
    users: 74,
    firstToken: 1.5,
    tokens: 396000
  },
  {
    date: '11/01',
    student: 58,
    knowledge: 115,
    learning: 108,
    other: 29,
    failed: 4,
    sessions: 102,
    users: 69,
    firstToken: 1.4,
    tokens: 372000
  },
  {
    date: '11/02',
    student: 128,
    knowledge: 198,
    learning: 164,
    other: 50,
    failed: 7,
    sessions: 174,
    users: 118,
    firstToken: 1.6,
    tokens: 648000
  },
  {
    date: '11/03',
    student: 136,
    knowledge: 214,
    learning: 176,
    other: 54,
    failed: 8,
    sessions: 186,
    users: 126,
    firstToken: 1.5,
    tokens: 696000
  },
  {
    date: '11/04',
    student: 142,
    knowledge: 228,
    learning: 183,
    other: 57,
    failed: 6,
    sessions: 198,
    users: 134,
    firstToken: 1.4,
    tokens: 732000
  },
  {
    date: '11/05',
    student: 154,
    knowledge: 246,
    learning: 198,
    other: 62,
    failed: 9,
    sessions: 213,
    users: 145,
    firstToken: 1.5,
    tokens: 792000
  },
  {
    date: '11/06',
    student: 168,
    knowledge: 258,
    learning: 216,
    other: 68,
    failed: 7,
    sessions: 228,
    users: 156,
    firstToken: 1.3,
    tokens: 852000
  },
  {
    date: '11/07',
    student: 92,
    knowledge: 188,
    learning: 174,
    other: 46,
    failed: 5,
    sessions: 164,
    users: 112,
    firstToken: 1.3,
    tokens: 600000
  },
  {
    date: '11/08',
    student: 86,
    knowledge: 176,
    learning: 166,
    other: 42,
    failed: 4,
    sessions: 152,
    users: 104,
    firstToken: 1.2,
    tokens: 564000
  }
]

export const requestCount = (day: (typeof aiDaily)[number]) =>
  day.student + day.knowledge + day.learning + day.other
