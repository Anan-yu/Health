import type { HealthProfile, Patient } from '@/types/api'

const questionnaireFields: readonly (keyof HealthProfile)[] = [
  'heightCm',
  'weightKg',
  'waistCm',
  'recentWeightChangeKg',
  'bloodType',
  'lifestyleSummary',
  'medicalHistory',
  'familyHistory',
  'allergyHistory',
  'currentMedications',
  'smokingStatus',
  'alcoholStatus',
  'exerciseFrequency',
  'sleepQuality',
  'sleepHours',
  'stressLevel',
  'moodStatus',
  'fearLevel',
  'dietaryPreference',
  'recentDietaryPattern',
  'diabetesStatus',
  'hypertensionStatus',
  'dyslipidemiaStatus',
  'fattyLiverStatus',
]

const hasValue = (value: unknown) => {
  if (typeof value === 'string') return value.trim() !== ''
  if (typeof value === 'number') return Number.isFinite(value)
  return value !== null && value !== undefined
}

/**
 * Calculate the same 27-field completeness shown to customers.
 * Keeping this fallback in the client prevents an old cached test API response
 * from hiding newly saved identity information until the server is restarted.
 */
export const calculateProfileCompleteness = (
  profile?: HealthProfile | null,
  patient?: Pick<Patient, 'name' | 'gender' | 'birthDate'> | null,
) => {
  const identityAnswered = [patient?.name, patient?.gender, patient?.birthDate].filter(hasValue).length
  const questionnaireAnswered = questionnaireFields.filter((field) => hasValue(profile?.[field])).length
  return Math.round(
    ((identityAnswered + questionnaireAnswered) / (questionnaireFields.length + 3)) * 100,
  )
}
