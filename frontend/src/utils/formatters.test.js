import { describe, expect, it } from 'vitest'
import { formatINR } from './formatters'

describe('formatINR', () => {
  it('formats whole numbers to INR with two decimals', () => {
    expect(formatINR(75)).toBe('₹75.00')
    expect(formatINR(1000)).toBe('₹1,000.00')
    expect(formatINR(100000)).toBe('₹1,00,000.00')
  })

  it('formats fractional values correctly', () => {
    expect(formatINR(75.5)).toBe('₹75.50')
    expect(formatINR(48.99)).toBe('₹48.99')
  })

  it('formats zero correctly', () => {
    expect(formatINR(0)).toBe('₹0.00')
    expect(formatINR('0')).toBe('₹0.00')
  })

  it('handles null, undefined, and empty string safely', () => {
    expect(formatINR(null)).toBe('₹0.00')
    expect(formatINR(undefined)).toBe('₹0.00')
    expect(formatINR('')).toBe('₹0.00')
  })

  it('handles invalid non-numeric values safely', () => {
    expect(formatINR('not-a-number')).toBe('₹0.00')
    expect(formatINR(NaN)).toBe('₹0.00')
  })

  it('formats numeric strings correctly', () => {
    expect(formatINR('120.5')).toBe('₹120.50')
  })
})
