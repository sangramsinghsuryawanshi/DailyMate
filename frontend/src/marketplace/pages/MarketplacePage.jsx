import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import MainLayout from '../../layouts/MainLayout'
import { getProviders } from '../services/marketplaceApi'

const categoryOptions = ['All', 'Electrician', 'Plumber', 'Mechanic', 'Tutor', 'Carpenter', 'Cleaner', 'Painter']
const sortOptions = ['Recommended', 'Rating', 'Price', 'Distance']

const categoryPricing = {
  Electrician: 60,
  Plumber: 55,
  Mechanic: 65,
  Tutor: 28,
  Carpenter: 50,
  Cleaner: 32,
  Painter: 48,
}

function deriveProviderMeta(provider, index) {
  const category = provider.category ?? 'Service'
  const rating = 4.6 + ((index + 1) % 4) * 0.1
  const distance = (1 + ((index * 13) % 8)) + (index % 2 ? 0.4 : 0.8)
  const basePrice = categoryPricing[category] ?? 40
  const price = basePrice + (index % 3) * 8

  return {
    ...provider,
    rating: Number(rating.toFixed(1)),
    distance: Number(distance.toFixed(1)),
    price,
    availability: index % 2 === 0 ? 'Available today' : 'Open this afternoon',
    verified: index % 3 !== 0,
  }
}

export default function MarketplacePage() {
  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['marketplace-providers'],
    queryFn: getProviders,
  })

  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('All')
  const [sortBy, setSortBy] = useState('Recommended')

  const enrichedProviders = useMemo(
    () => data.map(deriveProviderMeta),
    [data],
  )

  const visibleProviders = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()

    const filtered = enrichedProviders.filter((provider) => {
      const matchesQuery =
        !normalizedQuery ||
        [provider.name, provider.category, provider.description, provider.serviceArea].some((value) =>
          value?.toLowerCase().includes(normalizedQuery),
        )

      const matchesCategory = category === 'All' || provider.category === category
      return matchesQuery && matchesCategory
    })

    return [...filtered].sort((a, b) => {
      if (sortBy === 'Rating') return b.rating - a.rating
      if (sortBy === 'Price') return a.price - b.price
      if (sortBy === 'Distance') return a.distance - b.distance
      return b.rating * 10 - a.distance * 2 - a.price * 0.04
    })
  }, [category, enrichedProviders, query, sortBy])

  if (isLoading) {
    return (
      <MainLayout>
        <main className="page-state"><h1>Loading marketplace…</h1></main>
      </MainLayout>
    )
  }

  if (isError) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Marketplace unavailable</h1>
          <Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Local services</p>
          <h1>Find trusted help nearby</h1>
        </div>
        <Link to="/dashboard" className="btn btn-secondary">Back to dashboard</Link>
      </section>

      <section className="marketplace-toolbar panel">
        <label className="search-box search-wide">
          <span>What do you need help with?</span>
          <input
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Try plumber, tutor, cleaner..."
          />
        </label>

        <div className="marketplace-controls">
          <div className="pill-row" aria-label="Service categories">
            {categoryOptions.map((item) => (
              <button
                key={item}
                type="button"
                className={`pill ${category === item ? 'active' : ''}`}
                onClick={() => setCategory(item)}
              >
                {item}
              </button>
            ))}
          </div>

          <label className="field compact-field">
            <span>Sort by</span>
            <select value={sortBy} onChange={(event) => setSortBy(event.target.value)}>
              {sortOptions.map((option) => (
                <option key={option} value={option}>{option}</option>
              ))}
            </select>
          </label>
        </div>
      </section>

      <section className="marketplace-layout">
        <div className="panel panel-large">
          <div className="panel-header">
            <h2>Available near you</h2>
            <span>{visibleProviders.length} providers</span>
          </div>

          {visibleProviders.length === 0 ? (
            <div className="empty-state">
              <h3>No providers match your search.</h3>
              <p>Try a different keyword or reset the category filter to browse nearby services.</p>
              <button type="button" className="btn btn-primary" onClick={() => { setQuery(''); setCategory('All') }}>
                Reset filters
              </button>
            </div>
          ) : (
            <div className="provider-list">
              {visibleProviders.map((provider) => (
                <article key={provider.id} className="provider-card">
                  <div className="provider-avatar">{provider.name.slice(0, 1)}</div>

                  <div className="provider-copy">
                    <div className="provider-row">
                      <h3>{provider.name}</h3>
                      {provider.verified && <span className="verification-badge">Verified</span>}
                    </div>
                    <p className="muted">{provider.category}</p>
                    <p className="provider-description">{provider.description}</p>

                    <div className="meta-row">
                      <span>★ {provider.rating}</span>
                      <span>{provider.distance} km away</span>
                      <span>{provider.availability}</span>
                    </div>
                  </div>

                  <div className="provider-price">
                    <strong>From ${provider.price}</strong>
                    <Link to={`/marketplace/${provider.id}`} className="btn btn-small btn-primary">View profile</Link>
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>

        <aside className="side-stack">
          <div className="panel">
            <div className="panel-header">
              <h2>Popular requests</h2>
            </div>
            <div className="tag-cloud">
              <span>Plumber</span>
              <span>Math tutor</span>
              <span>Cleaner</span>
              <span>Electrician</span>
              <span>Painter</span>
              <span>Carpenter</span>
            </div>
          </div>

          <div className="panel">
            <div className="panel-header">
              <h2>Why DailyMate</h2>
            </div>
            <ul className="list-stack">
              <li>Verified local help</li>
              <li>Transparent pricing</li>
              <li>Fast, nearby options</li>
            </ul>
          </div>
        </aside>
      </section>
    </MainLayout>
  )
}
