import React, { useEffect, useState, useCallback } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import {
  ArrowLeft, BookOpen, Leaf, HelpCircle, Sparkles, Heart, CheckCircle, Play,
  AlertCircle, Lightbulb, Clock, TrendingUp, ExternalLink
} from 'lucide-react'

// ─── Full topic content database ────────────────────────────────────────────
const TOPIC_DATA = {
  'gum-care': {
    id: 'gum-care',
    title: 'Gum Care',
    subtitle: 'Healthy gums, healthy life',
    icon: Leaf,
    color: 'text-emerald-500',
    bg: 'bg-emerald-50 dark:bg-emerald-500/10',
    gradient: 'from-emerald-500 to-teal-600',
    readTime: '4 min read',
    content: [
      {
        heading: 'Why Gum Health Matters',
        body: 'Your gums are the foundation of your oral health. Healthy gums hold your teeth firmly in place and protect tooth roots from bacteria. Gum disease (periodontal disease) affects nearly half of adults and is closely linked to systemic conditions such as heart disease, diabetes, and even premature birth.'
      },
      {
        heading: 'Signs of Gum Disease',
        body: 'Watch for these early warning signs: gums that bleed when you brush or floss, red or swollen gums, persistent bad breath, gums that are pulling away from teeth (receding), or teeth that feel loose. Early stage gum disease (gingivitis) is completely reversible with proper care.'
      },
      {
        heading: 'How to Keep Gums Healthy',
        body: 'Brush gently along the gum line twice daily using a soft-bristled brush held at a 45-degree angle. Floss every day to remove bacteria and food from between teeth and below the gum line. Use an antimicrobial mouthwash. Avoid smoking, which dramatically increases gum disease risk. Schedule professional cleanings every 6 months.'
      },
      {
        heading: 'Gum Disease Stages',
        body: 'Stage 1 (Gingivitis): Inflammation of the gums, reversible with good hygiene. Stage 2 (Mild Periodontitis): Some bone loss begins. Stage 3 (Moderate Periodontitis): Significant bone and tissue loss. Stage 4 (Severe Periodontitis): Risk of tooth loss. Early detection is key.'
      }
    ],
    takeaways: [
      'Brush along the gum line at a 45-degree angle every day',
      'Floss daily — this is the single best way to prevent gum disease',
      'Bleeding gums when brushing is a warning sign, not normal',
      'See a dentist every 6 months for professional cleaning',
      'Quitting smoking cuts gum disease risk dramatically',
      'Gum disease is linked to heart disease and diabetes'
    ],
    watchLearn: [
      { title: 'Proper Gum Brushing Technique', desc: 'Learn the modified Bass technique for gum-line cleaning', videoUrl: 'https://www.youtube.com/results?search_query=proper+gum+brushing+technique' },
      { title: 'Flossing Step-by-Step', desc: 'A gentle C-shape around each tooth reaches below the gum line', videoUrl: 'https://www.youtube.com/results?search_query=how+to+floss+teeth+properly' },
      { title: 'Understanding Gingivitis vs Periodontitis', desc: 'Know the difference and when to seek professional help', videoUrl: 'https://www.youtube.com/results?search_query=gingivitis+vs+periodontitis+gum+disease' }
    ]
  },
  'tooth-sensitivity': {
    id: 'tooth-sensitivity',
    title: 'Tooth Sensitivity',
    subtitle: 'Causes and treatments',
    icon: HelpCircle,
    color: 'text-blue-500',
    bg: 'bg-blue-50 dark:bg-blue-500/10',
    gradient: 'from-blue-500 to-indigo-600',
    readTime: '3 min read',
    content: [
      {
        heading: 'What is Tooth Sensitivity?',
        body: 'Tooth sensitivity (dentinal hypersensitivity) is a sharp, sudden pain felt when teeth are exposed to cold, hot, sweet, or acidic stimuli. It occurs when the inner layer of the tooth (dentin) becomes exposed, allowing external sensations to reach the nerve inside the tooth through microscopic tubules.'
      },
      {
        heading: 'Common Causes',
        body: 'The most frequent causes include: enamel erosion from acidic foods and drinks, gum recession that exposes tooth roots, aggressive brushing with hard-bristled brushes, tooth grinding (bruxism), cracked teeth, tooth decay, and sensitivity after dental procedures such as whitening or fillings.'
      },
      {
        heading: 'Treatment Options',
        body: 'Desensitizing toothpaste containing potassium nitrate or stannous fluoride blocks tubules and reduces nerve signals. Fluoride treatments strengthen enamel. Dental bonding can cover exposed root surfaces. A night guard can protect against grinding. In severe cases, gum grafting may restore lost tissue.'
      },
      {
        heading: 'Prevention Tips',
        body: 'Use a soft-bristled toothbrush and avoid scrubbing aggressively. Limit consumption of acidic foods and drinks (sodas, citrus, vinegar). Wait 30 minutes after eating acidic foods before brushing. Use a fluoride rinse. If you grind your teeth at night, ask your dentist about a custom night guard.'
      }
    ],
    takeaways: [
      'Sensitivity means dentin is exposed — usually from enamel loss or gum recession',
      'Desensitizing toothpaste works — use it consistently for 2+ weeks',
      'Switch to a soft-bristled toothbrush immediately',
      'Rinse with water after acidic drinks; do not brush immediately',
      'Grinding teeth at night dramatically worsens sensitivity',
      'Persistent sensitivity needs professional evaluation — it can indicate a cavity or crack'
    ],
    watchLearn: [
      { title: 'Choosing the Right Toothbrush', desc: 'Soft bristles protect enamel and prevent gum recession', videoUrl: 'https://www.youtube.com/results?search_query=choosing+right+toothbrush+soft+bristles' },
      { title: 'Foods That Erode Enamel', desc: 'Discover which everyday foods are silently damaging your teeth', videoUrl: 'https://www.youtube.com/results?search_query=foods+that+erode+tooth+enamel' },
      { title: 'Night Guard Benefits', desc: 'How a simple appliance can protect your smile while you sleep', videoUrl: 'https://www.youtube.com/results?search_query=dental+night+guard+teeth+grinding+bruxism' }
    ]
  },
  'whitening-myths': {
    id: 'whitening-myths',
    title: 'Whitening Myths',
    subtitle: 'Facts vs fiction',
    icon: Sparkles,
    color: 'text-amber-500',
    bg: 'bg-amber-50 dark:bg-amber-500/10',
    gradient: 'from-amber-500 to-orange-600',
    readTime: '3 min read',
    content: [
      {
        heading: 'Myth vs Fact: Whitening Basics',
        body: 'Teeth whitening is one of the most misunderstood topics in dentistry. The reality: only the natural tooth structure (enamel and dentin) responds to whitening agents. Crowns, veneers, and fillings will NOT whiten and may appear darker than your natural teeth after treatment, requiring replacement if aesthetics matter.'
      },
      {
        heading: 'Do Whitening Strips Work?',
        body: 'Over-the-counter whitening strips containing hydrogen peroxide or carbamide peroxide do work — but only to a limited degree. They typically lighten teeth by 1–3 shades. Professional in-office treatments use higher concentrations and can achieve 6–8 shades of whitening in a single session.'
      },
      {
        heading: 'Charcoal Toothpaste Myth',
        body: 'Activated charcoal toothpaste is extremely abrasive and is not recommended by most dental associations. While it may remove surface stains temporarily, it also abrades enamel over time, leading to increased sensitivity and ironically making teeth appear more yellow as the yellow dentin beneath becomes more visible through thinner enamel.'
      },
      {
        heading: 'Safe Whitening Practices',
        body: 'Always consult your dentist before starting any whitening regimen. Use ADA-approved products. Limit whitening frequency — over-whitening can cause permanent sensitivity. Maintain results with good hygiene, avoid staining foods (coffee, tea, wine, berries), and quit tobacco.'
      }
    ],
    takeaways: [
      'Whitening only works on natural teeth — not crowns, veneers, or fillings',
      'OTC strips whiten 1–3 shades; professional treatments 6–8 shades',
      'Charcoal toothpaste can permanently damage enamel — avoid it',
      'Overuse of whitening products causes sensitivity and enamel damage',
      'Coffee, tea, wine, and tobacco are the main causes of tooth staining',
      'Whitening results last 6–18 months with good maintenance habits'
    ],
    watchLearn: [
      { title: 'How Whitening Agents Work', desc: 'The chemistry of hydrogen peroxide and enamel whitening', videoUrl: 'https://www.youtube.com/results?search_query=how+teeth+whitening+works+hydrogen+peroxide' },
      { title: 'Professional vs At-Home Whitening', desc: 'Compare effectiveness, safety, and cost', videoUrl: 'https://www.youtube.com/results?search_query=professional+vs+home+teeth+whitening+comparison' },
      { title: 'Maintaining a White Smile', desc: 'Habits and foods that preserve your whitening results', videoUrl: 'https://www.youtube.com/results?search_query=maintaining+white+teeth+habits+foods' }
    ]
  },
  'flossing': {
    id: 'flossing',
    title: 'Flossing',
    subtitle: 'Why, when and how to floss',
    icon: Heart,
    color: 'text-purple-500',
    bg: 'bg-purple-50 dark:bg-purple-500/10',
    gradient: 'from-purple-500 to-violet-600',
    readTime: '3 min read',
    content: [
      {
        heading: 'Why Flossing is Non-Negotiable',
        body: 'Your toothbrush can only reach about 60% of your tooth surfaces. The remaining 40% — the spaces between your teeth and below the gum line — can only be cleaned by flossing. These areas are where the most destructive dental diseases (cavities between teeth and gum disease) begin.'
      },
      {
        heading: 'When to Floss',
        body: 'The best time to floss is before bedtime. During sleep, saliva production drops and your mouth is less effective at neutralizing bacteria. Cleaning between teeth before sleep removes the food debris that bacteria would otherwise feast on overnight. Once per day is sufficient for most people.'
      },
      {
        heading: 'How to Floss Correctly',
        body: 'Use 18 inches of floss, wrapping most around each middle finger, leaving 2 inches to work with. Guide the floss between teeth using a gentle zigzag motion. Curve it into a C-shape around each tooth and slide gently below the gum line. Use a fresh section of floss for each tooth.'
      },
      {
        heading: 'Floss Alternatives',
        body: 'Water flossers (oral irrigators) are effective for people with braces, bridges, or those who struggle with traditional floss. Interdental brushes are excellent for larger gaps. Floss picks are convenient but less effective than traditional floss. Any flossing is better than none.'
      }
    ],
    takeaways: [
      'Floss reaches 40% of tooth surfaces that brushing misses',
      'Floss once daily — before bedtime is the ideal time',
      'Use the C-shape technique and go below the gum line gently',
      'Use a fresh section of floss for each tooth to avoid spreading bacteria',
      'Bleeding when you first start flossing is normal — it stops within 1–2 weeks',
      'Water flossers are a good alternative if traditional floss is difficult'
    ],
    watchLearn: [
      { title: 'The Correct Flossing Technique', desc: 'Step-by-step guide to flossing properly', videoUrl: 'https://www.youtube.com/results?search_query=correct+flossing+technique+step+by+step' },
      { title: 'Water Flosser vs Traditional Floss', desc: 'Which is better for your specific needs', videoUrl: 'https://www.youtube.com/results?search_query=water+flosser+vs+traditional+floss' },
      { title: 'Flossing with Braces', desc: 'Techniques and tools for orthodontic patients', videoUrl: 'https://www.youtube.com/results?search_query=how+to+floss+teeth+with+braces' }
    ]
  },
  'brushing-techniques': {
    id: 'brushing-techniques',
    title: 'Brushing Techniques',
    subtitle: 'Master the perfect brush',
    icon: BookOpen,
    color: 'text-cyan-500',
    bg: 'bg-cyan-50 dark:bg-cyan-500/10',
    gradient: 'from-cyan-500 to-blue-600',
    readTime: '4 min read',
    content: [
      {
        heading: 'The Modified Bass Technique',
        body: 'The gold-standard brushing method recommended by most dental professionals. Hold your brush at a 45-degree angle to your gum line. Use short, gentle horizontal strokes (about the width of one tooth). Apply light pressure — brushing harder does not clean better and damages enamel and gums.'
      },
      {
        heading: 'Two Full Minutes, Twice Daily',
        body: 'Research consistently shows that most people brush for only 45–70 seconds. Dentists recommend 2 full minutes, twice daily. Divide your mouth into four quadrants — upper right, upper left, lower right, lower left — and spend 30 seconds on each. A timer or an electric brush with a built-in timer helps significantly.'
      },
      {
        heading: 'Choosing the Right Brush',
        body: 'Always choose a soft-bristled brush. Medium and hard bristles can damage both enamel and gum tissue. Replace your brush every 3 months or when bristles become frayed. An electric toothbrush removes significantly more biofilm than manual brushing due to oscillation and consistent movement.'
      },
      {
        heading: 'Brushing All Surfaces',
        body: 'Cover all tooth surfaces: the outer surface facing the cheek, the inner surface facing the tongue/palate, and the chewing (occlusal) surface. Tilt the brush vertically for the inner surfaces of front teeth. Finish by brushing your tongue to remove odour-causing bacteria.'
      }
    ],
    takeaways: [
      'Use a soft-bristled brush — medium/hard bristles damage enamel',
      'Brush at a 45-degree angle to the gum line with gentle pressure',
      'Brush for a full 2 minutes, not 30–40 seconds',
      'Replace your toothbrush every 3 months or when bristles fray',
      'Electric toothbrushes are significantly more effective than manual',
      'Brush your tongue — it harbours bacteria that cause bad breath'
    ],
    watchLearn: [
      { title: 'The Modified Bass Brushing Technique', desc: 'The dentist-recommended method explained step by step', videoUrl: 'https://www.youtube.com/results?search_query=modified+bass+brushing+technique+dentist' },
      { title: 'Manual vs Electric Toothbrush', desc: 'Research-backed comparison of cleaning effectiveness', videoUrl: 'https://www.youtube.com/results?search_query=manual+vs+electric+toothbrush+comparison' },
      { title: 'Common Brushing Mistakes', desc: 'Avoid these habits that are silently damaging your teeth', videoUrl: 'https://www.youtube.com/results?search_query=common+toothbrushing+mistakes+to+avoid' }
    ]
  }
}

// ─── Component ───────────────────────────────────────────────────────────────
export default function EducationDetail() {
  const { topicId } = useParams()
  const navigate = useNavigate()
  const [toast, setToast] = useState('')

  // Show a brief toast notification
  const showToast = useCallback((msg) => {
    setToast(msg)
    setTimeout(() => setToast(''), 3000)
  }, [])

  // Handle Watch & Learn card click
  const handleWatchLearn = useCallback((item) => {
    console.log('[DentNova] WATCH_LEARN_CLICKED:', item.title)
    if (item.videoUrl) {
      console.log('[DentNova] EDUCATION_VIDEO_URL:', item.videoUrl)
      window.open(item.videoUrl, '_blank', 'noopener,noreferrer')
    } else {
      showToast('Video resource coming soon.')
    }
  }, [showToast])

  useEffect(() => {
    console.log('[DentNova] EDUCATION_TOPIC_CLICKED:', topicId)
    console.log('[DentNova] EDUCATION_TOPIC_ID:', topicId)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }, [topicId])

  const topic = TOPIC_DATA[topicId]

  if (!topic) {
    return (
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16 text-center">
        <AlertCircle className="w-12 h-12 text-slate-400 mx-auto mb-4" />
        <h2 className="text-xl font-bold text-slate-800 dark:text-white mb-2">Topic Not Found</h2>
        <p className="text-slate-500 dark:text-slate-400 mb-6">This topic doesn't exist or may have been removed.</p>
        <Link
          to="/education"
          className="inline-flex items-center gap-2 px-6 py-3 bg-cyan-500 hover:bg-cyan-600 text-white rounded-xl font-bold text-sm transition"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Education
        </Link>
      </div>
    )
  }

  const Icon = topic.icon

  // Log when detail fully renders
  useEffect(() => {
    if (topic) {
      console.log('[DentNova] EDUCATION_DETAIL_LOADED:', topic.title)
    }
  }, [topic])

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 bg-slate-50 dark:bg-slate-950 transition-colors duration-300">

      {/* Back Button */}
      <button
        onClick={() => navigate('/education')}
        className="flex items-center gap-2 text-sm font-semibold text-slate-500 dark:text-slate-400 hover:text-cyan-500 dark:hover:text-cyan-400 transition-colors"
      >
        <ArrowLeft className="w-4 h-4" />
        Back to Education
      </button>

      {/* Hero Header */}
      <div className={`relative overflow-hidden rounded-3xl bg-gradient-to-br ${topic.gradient} p-8 text-white shadow-lg`}>
        <div className="absolute top-0 right-0 w-48 h-48 bg-white/5 rounded-full blur-3xl" />
        <div className="absolute bottom-0 left-0 w-32 h-32 bg-black/10 rounded-full blur-2xl" />
        <div className="relative z-10 space-y-3">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-white/20 backdrop-blur-sm flex items-center justify-center">
              <Icon className="w-6 h-6 text-white" />
            </div>
            <div className="flex items-center gap-2 text-xs font-bold bg-white/20 px-3 py-1.5 rounded-full">
              <Clock className="w-3.5 h-3.5" />
              {topic.readTime}
            </div>
          </div>
          <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight">{topic.title}</h1>
          <p className="text-white/80 text-base">{topic.subtitle}</p>
        </div>
      </div>

      {/* Main Content Sections */}
      <div className="space-y-6">
        {topic.content.map((section, idx) => (
          <div
            key={idx}
            className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-6 shadow-sm hover:shadow-md transition-shadow duration-200"
          >
            <div className="flex items-start gap-3 mb-3">
              <div className="w-7 h-7 rounded-lg bg-cyan-500/10 dark:bg-cyan-500/20 flex items-center justify-center shrink-0 mt-0.5">
                <BookOpen className="w-4 h-4 text-cyan-500" />
              </div>
              <h2 className="text-lg font-bold text-slate-900 dark:text-white">{section.heading}</h2>
            </div>
            <p className="text-sm text-slate-600 dark:text-slate-300 leading-relaxed pl-10">
              {section.body}
            </p>
          </div>
        ))}
      </div>

      {/* Key Takeaways */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 sm:p-8 shadow-sm">
        <div className="flex items-center gap-2 mb-6 border-b border-slate-100 dark:border-slate-800 pb-4">
          <Lightbulb className="w-5 h-5 text-amber-500" />
          <h2 className="text-base font-bold text-slate-900 dark:text-white">Key Takeaways</h2>
        </div>
        <ul className="space-y-3">
          {topic.takeaways.map((item, idx) => (
            <li key={idx} className="flex items-start gap-3">
              <CheckCircle className="w-5 h-5 text-emerald-500 shrink-0 mt-0.5" />
              <span className="text-sm text-slate-700 dark:text-slate-300 leading-relaxed">{item}</span>
            </li>
          ))}
        </ul>
      </div>

      {/* Watch & Learn Section */}
      <div className="space-y-4">
        <div className="flex items-center gap-2">
          <TrendingUp className="w-5 h-5 text-cyan-500" />
          <h2 className="text-base font-bold text-slate-900 dark:text-white">Watch &amp; Learn</h2>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {topic.watchLearn.map((item, idx) => (
            <button
              key={idx}
              onClick={() => handleWatchLearn(item)}
              className="group text-left bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm hover:shadow-lg hover:border-cyan-500/40 dark:hover:border-cyan-500/40 hover:-translate-y-1 active:translate-y-0 active:shadow-sm transition-all duration-200 cursor-pointer w-full"
            >
              <div className="w-10 h-10 rounded-xl bg-cyan-500/10 dark:bg-cyan-500/20 flex items-center justify-center mb-4 group-hover:bg-cyan-500/25 transition-colors">
                <Play className="w-5 h-5 text-cyan-500 fill-cyan-500/50 group-hover:fill-cyan-500/80 transition-all" />
              </div>
              <div className="flex items-start justify-between gap-1 mb-1">
                <h3 className="text-sm font-bold text-slate-800 dark:text-slate-200 group-hover:text-cyan-500 dark:group-hover:text-cyan-400 transition-colors leading-snug">
                  {item.title}
                </h3>
                <ExternalLink className="w-3.5 h-3.5 text-slate-300 dark:text-slate-600 group-hover:text-cyan-400 transition-colors shrink-0 mt-0.5" />
              </div>
              <p className="text-xs text-slate-400 dark:text-slate-500 leading-relaxed">{item.desc}</p>
            </button>
          ))}
        </div>
      </div>

      {/* Toast notification */}
      {toast && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 bg-slate-800 dark:bg-slate-700 text-white text-sm font-medium px-5 py-3 rounded-2xl shadow-2xl flex items-center gap-2 animate-fade-in">
          <AlertCircle className="w-4 h-4 text-amber-400 shrink-0" />
          {toast}
        </div>
      )}

      {/* Navigation Footer */}
      <div className="flex items-center justify-between pt-4 border-t border-slate-200 dark:border-slate-800">
        <button
          onClick={() => navigate('/education')}
          className="flex items-center gap-2 px-5 py-2.5 border border-slate-200 dark:border-slate-800 rounded-xl text-sm font-semibold text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-850 transition"
        >
          <ArrowLeft className="w-4 h-4" />
          All Topics
        </button>
        <span className="text-xs text-slate-400 font-medium">
          🦷 DentNova Education Suite
        </span>
      </div>

    </div>
  )
}
