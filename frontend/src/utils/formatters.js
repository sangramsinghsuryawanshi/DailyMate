/**
 * Centralized currency formatter for DailyMate.
 * Formats numbers into Indian Rupee (INR / ₹) representation.
 *
 * @param {number|string|null|undefined} value
 * @returns {string} Formatted INR currency string (e.g. ₹75.00)
 */
export const formatINR = (value) => {
  if (value === null || value === undefined || value === '') {
    return '₹0.00'
  }
  const num = Number(value)
  if (isNaN(num)) {
    return '₹0.00'
  }
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(num)
}
