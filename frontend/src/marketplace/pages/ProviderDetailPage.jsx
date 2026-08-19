import { useMemo } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import MainLayout from '../../layouts/MainLayout'
import { getProviders } from '../services/marketplaceApi'

const categoryPricing = {
  Electrician: 60,
  Plumber: 55,
  Mechanic: 65,
  Tutor: 28,
  Carpenter: 50,
  Cleaner: 32,
  Painter: 48,
}

function enrichProvider(provider, index) {
  const category = provider.category || 'Service'
  const rating = 4.6 + ((index + 1) % 4) * 0.1
  const basePrice = categoryPricing[category] || 40
  const price = basePrice + (index % 3) * 8

  return {
    ...provider,
    rating: Number(rating.toFixed(1)),
    price,
    distance: 1 + ((index * 13) % 8) + (index % 2 ? 0.4 : 0.8),
    verified: index % 3 !== 0,
    availability: index % 2 === 0 ? 'Available today' : 'Open this afternoon',
    experience: `${2 + ((index + 1) % 6)} years`,
    serviceArea: provider.serviceArea || 'Downtown area',
    reviewCount: 32 + index * 7,
  }
}

export default function ProviderDetailPage() {
  const { id } = useParams()
  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['marketplace-providers'],
    queryFn: getProviders,
  })

  const provider = useMemo(() => {
    const index = data.findIndex((item) => item.id === id)
    return index >= 0 ? enrichProvider(data[index], index) : null
  }, [data, id])

  if (isLoading) {
    return (
      <MainLayout>
        <main className="page-state"><h1>Loading provider profile…</h1></main>
      </MainLayout>
    )
  }

  if (isError || !provider) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Provider not found</h1>
          <Link to="/marketplace" className="btn btn-primary">Back to marketplace</Link>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Provider profile</p>
          <h1>{provider.name}</h1>
        </div>
        <Link to="/marketplace" className="btn btn-secondary">Back to results</Link>
      </section>

      <section className="detail-layout">
        <div className="panel panel-large provider-hero">
          <div className="provider-avatar large-avatar">{provider.name.slice(0, 1)}</div>
          <div className="provider-identity">
            <div className="provider-row">
              <h2>{provider.name}</h2>
              {provider.verified && <span className="verification-badge">Verified</span>}
            </div>
            <p className="muted">{provider.category} · {provider.serviceArea}</p>
            <div className="meta-row">
              <span>★ {provider.rating}</span>
              <span>{provider.reviewCount}+ reviews</span>
              <span>{provider.distance} km away</span>
            </div>
          </div>
          <div className="provider-price detail-price">
            <strong>From ${provider.price}</strong>
            <button type="button" className="btn btn-primary">Request service</button>
          </div>
        </div>

        <div className="detail-grid">
          <div className="panel">
            <div className="panel-header">
              <h2>About</h2>
            </div>
            <p className="detail-copy">{provider.description}</p>
          </div>

          <div className="panel">
            <div className="panel-header">
              <h2>Quick facts</h2>
            </div>
            <ul className="detail-list">
              <li><strong>Experience:</strong> {provider.experience}</li>
              <li><strong>Availability:</strong> {provider.availability}</li>
              <li><strong>Location:</strong> {provider.serviceArea}</li>
              <li><strong>Pricing:</strong> From ${provider.price}</li>
            </ul>
          </div>
        </div>

        <div className="panel">
          <div className="panel-header">
            <h2>Reviews</h2>
          </div>
          <div className="review-list">
            <article className="review-item">
              <strong>Marina R.</strong>
              <p>“Very professional and reliable. Help arrived quickly and the work was clean.”</p>
            </article>
            <article className="review-item">
              <strong>Daniel K.</strong>
              <p>“Clear communication and fair pricing. Easy to book and very helpful.”</p>
            </article>
          </div>
        </div>
      </section>
    </MainLayout>
  )
}
