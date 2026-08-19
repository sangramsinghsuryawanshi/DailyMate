export function searchDailyMate(query = '', providers = [], extraItems = []) {
  const normalizedQuery = query.trim().toLowerCase()

  if (!normalizedQuery) {
    return []
  }

  const entries = [
    ...providers.map((provider) => ({
      id: provider.id,
      title: provider.name,
      category: provider.category ?? 'Service',
      description: provider.description ?? 'Local service provider',
      to: `/marketplace/${provider.id}`,
      tags: [provider.category, provider.serviceArea, provider.name],
    })),
    ...extraItems.map((item) => ({
      ...item,
      tags: item.tags ?? [item.category],
    })),
  ]

  return entries
    .filter((entry) => {
      const haystack = [
        entry.title,
        entry.category,
        entry.description,
        ...(entry.tags ?? []),
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()

      return haystack.includes(normalizedQuery)
    })
    .slice(0, 6)
}
