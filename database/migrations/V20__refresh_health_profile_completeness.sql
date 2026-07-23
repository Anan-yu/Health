-- Recalculate legacy records that were created before the expanded health questionnaire.
UPDATE health_profile
SET profile_completeness = ROUND(
  100 * (
    (height_cm IS NOT NULL) +
    (weight_kg IS NOT NULL) +
    (blood_type IS NOT NULL AND TRIM(blood_type) <> '') +
    (lifestyle_summary IS NOT NULL AND TRIM(lifestyle_summary) <> '') +
    (medical_history IS NOT NULL AND TRIM(medical_history) <> '') +
    (family_history IS NOT NULL AND TRIM(family_history) <> '') +
    (allergy_history IS NOT NULL AND TRIM(allergy_history) <> '') +
    (current_medications IS NOT NULL AND TRIM(current_medications) <> '') +
    (smoking_status IS NOT NULL AND TRIM(smoking_status) <> '') +
    (alcohol_status IS NOT NULL AND TRIM(alcohol_status) <> '') +
    (exercise_frequency IS NOT NULL AND TRIM(exercise_frequency) <> '') +
    (sleep_quality IS NOT NULL AND TRIM(sleep_quality) <> '') +
    (sleep_hours IS NOT NULL) +
    (stress_level IS NOT NULL AND TRIM(stress_level) <> '') +
    (mood_status IS NOT NULL AND TRIM(mood_status) <> '') +
    (fear_level IS NOT NULL AND TRIM(fear_level) <> '') +
    (dietary_preference IS NOT NULL AND TRIM(dietary_preference) <> '') +
    (recent_dietary_pattern IS NOT NULL AND TRIM(recent_dietary_pattern) <> '') +
    (diabetes_status IS NOT NULL AND TRIM(diabetes_status) <> '') +
    (hypertension_status IS NOT NULL AND TRIM(hypertension_status) <> '') +
    (dyslipidemia_status IS NOT NULL AND TRIM(dyslipidemia_status) <> '') +
    (fatty_liver_status IS NOT NULL AND TRIM(fatty_liver_status) <> '')
  ) / 22
);
