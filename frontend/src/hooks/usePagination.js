import { useState, useCallback, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'

export function usePagination({
  initialPage = 0,
  initialPageSize = 20,
  syncWithUrl = false,
  pageParamName = 'page',
  sizeParamName = 'size',
} = {}) {
  const [searchParams, setSearchParams] = useSearchParams()

  const readUrlInt = useCallback(
    (paramName, defaultVal) => {
      if (!syncWithUrl) return defaultVal
      const val = searchParams.get(paramName)
      if (val != null && !isNaN(Number(val))) {
        return Math.max(0, parseInt(val, 10))
      }
      return defaultVal
    },
    [syncWithUrl, searchParams]
  )

  const [page, setPageState] = useState(() => readUrlInt(pageParamName, initialPage))
  const [pageSize, setPageSizeState] = useState(() => readUrlInt(sizeParamName, initialPageSize))

  // Update URL if syncWithUrl is active
  const updateUrl = useCallback(
    (newPage, newSize) => {
      if (!syncWithUrl) return
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev)
        if (newPage === 0) {
          next.delete(pageParamName)
        } else {
          next.set(pageParamName, String(newPage))
        }

        if (newSize === initialPageSize) {
          next.delete(sizeParamName)
        } else {
          next.set(sizeParamName, String(newSize))
        }
        return next
      })
    },
    [syncWithUrl, setSearchParams, pageParamName, sizeParamName, initialPageSize]
  )

  const setPage = useCallback(
    (newPage) => {
      const clamped = Math.max(0, newPage)
      setPageState(clamped)
      updateUrl(clamped, pageSize)
    },
    [pageSize, updateUrl]
  )

  const setPageSize = useCallback(
    (newSize) => {
      const clamped = Math.max(1, Math.min(100, newSize))
      setPageSizeState(clamped)
      setPageState(0) // Reset to page 0 on size change
      updateUrl(0, clamped)
    },
    [updateUrl]
  )

  const resetPage = useCallback(() => {
    setPageState(0)
    updateUrl(0, pageSize)
  }, [pageSize, updateUrl])

  return {
    page,
    pageSize,
    setPage,
    setPageSize,
    resetPage,
  }
}

export default usePagination
