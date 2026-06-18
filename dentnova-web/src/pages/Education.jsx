import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { 
  BookOpen, Sparkles, AlertCircle, HelpCircle, CheckCircle, ArrowRight,
  TrendingUp, Leaf, Award, Heart, HelpCircle as HelpIcon, RotateCw
} from 'lucide-react'

// Facts and questions from EducationActivity.java
const DENTAL_FACTS = [
  "Saliva helps protect your teeth by neutralizing acids from food.",
  "Tooth enamel is the hardest substance your body produces.",
  "The average person brushes for only 45–70 seconds — dentists recommend 2 full minutes.",
  "Flossing removes up to 40% of plaque that your toothbrush misses.",
  "Your mouth hosts over 700 species of bacteria — most are harmless.",
  "A tooth can survive outside the mouth if kept in milk within 30 minutes of being knocked out.",
  "Gum disease (periodontitis) is linked to heart disease and diabetes.",
  "Fluoride strengthens enamel by replacing minerals lost to acid attacks.",
  "Children lose their first baby tooth around age 6–7 years.",
  "Drinking water after sugary food or drinks helps rinse away cavity-causing acids.",
  "Electric toothbrushes remove up to 21% more plaque than manual ones.",
  "Grinding your teeth (bruxism) can wear enamel down by up to 25% over time."
]

const ARTICLES = [
  { id: 'gum-care', title: "Gum care", desc: "Healthy gums, healthy life", icon: Leaf, color: "text-emerald-500", bg: "bg-emerald-50 dark:bg-emerald-500/10" },
  { id: 'tooth-sensitivity', title: "Tooth sensitivity", desc: "Causes and treatments", icon: HelpCircle, color: "text-blue-500", bg: "bg-blue-50 dark:bg-blue-500/10" },
  { id: 'whitening-myths', title: "Whitening myths", desc: "Facts vs fiction", icon: Sparkles, color: "text-amber-500", bg: "bg-amber-50 dark:bg-amber-500/10" },
  { id: 'flossing', title: "Flossing", desc: "Why, when and how to floss", icon: Heart, color: "text-purple-500", bg: "bg-purple-50 dark:bg-purple-500/10" },
  { id: 'brushing-techniques', title: "Brushing techniques", desc: "Master the perfect brush", icon: BookOpen, color: "text-cyan-500", bg: "bg-cyan-50 dark:bg-cyan-500/10" }
]

const QUIZ_QUESTIONS = [
  {
    question: "What is the primary cause of tooth decay?",
    options: ["Acid from bacteria eating sugar", "Drinking too much water", "Eating crisp vegetables", "Brushing too hard"],
    correct: 0,
    explanation: "Tooth decay is caused by acid produced when mouth bacteria feed on dietary sugars."
  },
  {
    question: "What is dental plaque?",
    options: ["Stained tooth enamel", "A sticky film of bacteria", "Hardened calcium deposit", "Food debris"],
    correct: 1,
    explanation: "Plaque is a sticky, colorless film of bacteria that constantly forms on your teeth."
  },
  {
    question: "What is the first stage of gum disease?",
    options: ["Periodontitis", "Gingivitis", "Enamel loss", "Tooth decay"],
    correct: 1,
    explanation: "Gingivitis is the early, reversible stage of gum disease, marked by red, swollen gums that bleed easily."
  },
  {
    question: "Why is flossing daily necessary?",
    options: ["It whitens teeth", "It cleans areas a toothbrush cannot reach", "It strengthens jaw bone", "It replaces brushing"],
    correct: 1,
    explanation: "Flossing removes plaque and food particles from tight spaces between teeth that toothbrush bristles miss."
  },
  {
    question: "How does therapeutic mouthwash help?",
    options: ["Replaces the need to floss", "Reduces plaque and kills bacteria", "Replaces the need to brush", "Whitens teeth instantly"],
    correct: 1,
    explanation: "Mouthwash helps reduce bacteria, prevent cavities, and reach areas that brushing and flossing might miss."
  },
  {
    question: "Why does sugar lead to cavities?",
    options: ["Sugar dissolves enamel directly", "Bacteria turn sugar into harmful acid", "Sugar stains teeth yellow", "Sugar blocks saliva flow"],
    correct: 1,
    explanation: "Bacteria in the mouth feed on sugar and produce acid, which attacks and weakens tooth enamel."
  },
  {
    question: "How often should you see a dentist for checkups?",
    options: ["Every 6 months", "Only when in pain", "Every 2 years", "Once every 5 years"],
    correct: 0,
    explanation: "Regular visits every 6 months help detect and prevent issues before they become serious."
  },
  {
    question: "How long should you brush your teeth?",
    options: ["30 seconds", "1 minute", "2 minutes", "5 minutes"],
    correct: 2,
    explanation: "Dentists recommend brushing for at least 2 minutes twice a day for effective cleaning."
  },
  {
    question: "Which of these is a key risk factor for oral cancer?",
    options: ["Tobacco and heavy alcohol use", "Drinking cold water", "Using fluoride toothpaste", "Eating dairy products"],
    correct: 0,
    explanation: "Tobacco use of any kind and heavy alcohol consumption significantly increase the risk of oral cancer."
  },
  {
    question: "What can cause sudden tooth sensitivity?",
    options: ["Exposed dentin from receded gums", "Stronger tooth enamel", "Using a soft toothbrush", "Drinking tap water"],
    correct: 0,
    explanation: "When gum tissue recedes, the underlying dentin layer is exposed, leading to temperature sensitivity."
  },
  {
    question: "Why do wisdom teeth often need removal?",
    options: ["They have no enamel", "They often get impacted due to lack of space", "They cause bad breath", "They are too small to clean"],
    correct: 1,
    explanation: "Wisdom teeth can become trapped or impacted if there is not enough room in the jaw for them to erupt."
  },
  {
    question: "What role does fluoride play in toothpaste?",
    options: ["It replaces flossing", "It strengthens enamel and prevents cavities", "It makes teeth shiny", "It freshens breath"],
    correct: 1,
    explanation: "Fluoride remineralizes weakened tooth enamel, making it more resistant to future acid attacks."
  },
  {
    question: "Which toothpaste ingredient helps with tooth sensitivity?",
    options: ["Potassium nitrate", "Activated charcoal", "Baking soda", "Hydrogen peroxide"],
    correct: 0,
    explanation: "Potassium nitrate blocks pathways from the tooth surface to the nerve, reducing sensitivity."
  },
  {
    question: "What is a dental cavity?",
    options: ["A stained spot", "A permanent hole in a tooth", "A swollen gum area", "A loose tooth root"],
    correct: 1,
    explanation: "A cavity is a permanently damaged area in the hard surface of your tooth that develops into a tiny hole."
  },
  {
    question: "What is the best foundation for great oral hygiene?",
    options: ["Brushing, flossing, and regular checkups", "Using whitening strips daily", "Avoiding all solid foods", "Drinking mouthwash only"],
    correct: 0,
    explanation: "Combining twice-daily brushing, daily flossing, and twice-yearly checkups is the gold standard for oral health."
  }
]

export default function Education() {
  const navigate = useNavigate()
  const [fact, setFact] = useState('')
  const [fadeFact, setFadeFact] = useState(false)
  const [quizIdx, setQuizIdx] = useState(0)
  const [answered, setAnswered] = useState(false)
  const [selectedOpt, setSelectedOpt] = useState(-1)
  const [lastFactIdx, setLastFactIdx] = useState(-1)

  useEffect(() => {
    pickRandomFact()
    pickRandomQuiz()
  }, [])

  const pickRandomFact = () => {
    setFadeFact(true)
    setTimeout(() => {
      let idx = Math.floor(Math.random() * DENTAL_FACTS.length)
      if (DENTAL_FACTS.length > 1 && idx === lastFactIdx) {
        idx = (idx + 1) % DENTAL_FACTS.length
      }
      setLastFactIdx(idx)
      setFact(DENTAL_FACTS[idx])
      setFadeFact(false)
    }, 150)
  }

  const pickRandomQuiz = () => {
    const idx = Math.floor(Math.random() * QUIZ_QUESTIONS.length)
    setQuizIdx(idx)
    setAnswered(false)
    setSelectedOpt(-1)
  }

  const handleSelectOpt = (optIdx) => {
    if (answered) return
    setSelectedOpt(optIdx)
    setAnswered(true)
  }

  const currentQuiz = QUIZ_QUESTIONS[quizIdx]
  const isCorrect = selectedOpt === currentQuiz?.correct

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 bg-slate-50 dark:bg-slate-950 transition-colors duration-300">
      
      {/* Page Title */}
      <div className="space-y-1">
        <h2 className="text-2xl font-extrabold text-slate-900 dark:text-white flex items-center gap-2">
          <BookOpen className="w-6 h-6 text-cyan-500" />
          Education Suite
        </h2>
        <p className="text-xs text-slate-400 font-medium">Learn dental care tips and test your oral hygiene knowledge.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Facts & Quiz Area */}
        <div className="lg:col-span-2 space-y-8">
          
          {/* Did You Know? Fact Card */}
          <div 
            onClick={pickRandomFact}
            className="group p-6 bg-gradient-to-r from-cyan-500 to-blue-600 rounded-3xl text-white shadow-md hover:shadow-lg cursor-pointer transition-all duration-300 relative overflow-hidden select-none"
          >
            <div className="absolute top-0 right-0 w-32 h-32 bg-white/5 rounded-full blur-2xl group-hover:scale-110 transition-transform"></div>
            <div className="flex justify-between items-start mb-3">
              <span className="text-xs font-bold uppercase tracking-wider bg-white/20 px-2.5 py-1 rounded-full flex items-center gap-1.5">
                💡 Did You Know?
              </span>
              <RotateCw className="w-4 h-4 opacity-50 group-hover:opacity-100 group-hover:rotate-45 transition-all" />
            </div>
            
            <p className={`text-lg font-semibold leading-relaxed transition-opacity duration-150 ${fadeFact ? 'opacity-0' : 'opacity-100'}`}>
              "{fact}"
            </p>
            <span className="text-[10px] text-cyan-100 block mt-4 font-semibold uppercase tracking-wider">Tap card to load another dental fact ✨</span>
          </div>

          {/* Quick Quiz Card */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 sm:p-8 shadow-sm space-y-6 transition duration-300">
            <div className="flex items-center gap-2 border-b border-slate-100 dark:border-slate-800 pb-3">
              <HelpIcon className="w-5 h-5 text-cyan-500" />
              <h3 className="font-bold text-slate-800 dark:text-slate-200 text-sm">Quick Quiz</h3>
            </div>

            {currentQuiz && (
              <div className="space-y-6">
                <h4 className="text-lg font-extrabold text-slate-800 dark:text-white leading-snug">
                  {currentQuiz.question}
                </h4>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {currentQuiz.options.map((opt, i) => {
                    const isSelected = selectedOpt === i
                    const isRightOpt = i === currentQuiz.correct
                    
                    let optStyle = "border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-850"
                    if (answered) {
                      if (isRightOpt) {
                        optStyle = "border-emerald-500 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                      } else if (isSelected) {
                        optStyle = "border-red-500 bg-red-500/10 text-red-600 dark:text-red-400"
                      } else {
                        optStyle = "border-slate-100 dark:border-slate-850 opacity-50 cursor-not-allowed"
                      }
                    }

                    return (
                      <button
                        key={i}
                        disabled={answered}
                        onClick={() => handleSelectOpt(i)}
                        className={`p-4 rounded-xl border text-sm font-semibold text-left transition-all ${optStyle}`}
                      >
                        {opt}
                      </button>
                    )
                  })}
                </div>

                {answered && (
                  <div className="p-5 bg-slate-50 dark:bg-slate-950 rounded-2xl border border-slate-150 dark:border-slate-850 space-y-3 transition duration-300">
                    <div className="flex items-center gap-2 text-sm font-bold">
                      {isCorrect ? (
                        <>
                          <CheckCircle className="w-5 h-5 text-emerald-500" />
                          <span className="text-emerald-600 dark:text-emerald-400">✅ Correct! Excellent!</span>
                        </>
                      ) : (
                        <>
                          <AlertCircle className="w-5 h-5 text-red-500" />
                          <span className="text-red-600 dark:text-red-400">💡 Good Try! Keep learning!</span>
                        </>
                      )}
                    </div>
                    <p className="text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
                      {currentQuiz.explanation}
                    </p>
                    <button
                      onClick={pickRandomQuiz}
                      className="px-4 py-2 bg-cyan-500 hover:bg-cyan-600 text-white rounded-lg text-xs font-bold shadow-sm transition mt-2 flex items-center gap-1.5"
                    >
                      Next Question
                      <ArrowRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Articles Sidebar */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-sm flex flex-col justify-between transition duration-300">
          <div className="space-y-6">
            <div className="flex items-center gap-2 border-b border-slate-100 dark:border-slate-800 pb-3">
              <BookOpen className="w-5 h-5 text-slate-400" />
              <h3 className="font-bold text-slate-850 dark:text-slate-200 text-sm">Learning Guides</h3>
            </div>

            <div className="space-y-4">
              {ARTICLES.map((art, i) => (
                <div
                  key={i}
                  onClick={() => {
                    console.log('[DentNova] EDUCATION_TOPIC_CLICKED:', art.title)
                    console.log('[DentNova] EDUCATION_TOPIC_ID:', art.id)
                    navigate(`/education/${art.id}`)
                  }}
                  className="flex items-center gap-4 p-3.5 hover:bg-slate-50 dark:hover:bg-slate-850 rounded-2xl cursor-pointer group transition-colors duration-200 hover:shadow-sm"
                >
                  <div className={`w-11 h-11 rounded-xl flex items-center justify-center shrink-0 ${art.bg} ${art.color}`}>
                    <art.icon className="w-5 h-5" />
                  </div>
                  <div className="flex-grow min-w-0">
                    <h4 className="text-sm font-bold text-slate-800 dark:text-slate-200 group-hover:text-cyan-500 transition-colors truncate">
                      {art.title}
                    </h4>
                    <p className="text-xs text-slate-400 truncate">{art.desc}</p>
                  </div>
                  <ArrowRight className="w-4 h-4 text-slate-300 dark:text-slate-600 group-hover:text-cyan-500 group-hover:translate-x-1 transition-all shrink-0" />
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
