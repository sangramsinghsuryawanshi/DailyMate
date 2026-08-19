import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { createDonationCenter, deleteDonationCenter, getDonationCenters, updateDonationCenter } from '../services/bloodApi'

const defaultForm = {
  name: '',
  location: '',
  contact: '',
  description: '',
}

export default function BloodDonationPage() {
  const queryClient = useQueryClient()
  const [form, setForm] = useState(defaultForm)
  const [editingId, setEditingId] = useState(null)

  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['blood-centers'],
    queryFn: getDonationCenters,
  })

  const saveMutation = useMutation({
    mutationFn: (payload) => editingId ? updateDonationCenter(editingId, payload) : createDonationCenter(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['blood-centers'] })
      setForm(defaultForm)
      setEditingId(null)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteDonationCenter,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['blood-centers'] }),
  })

  function handleChange(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    saveMutation.mutate(form)
  }

  function handleEdit(center) {
    setEditingId(center.id)
    setForm({
      name: center.name,
      location: center.location,
      contact: center.contact,
      description: center.description,
    })
  }

  if (isLoading) return <main className="dashboard"><h1>Loading donation centers…</h1></main>
  if (isError) return <main className="dashboard"><h1>Donation centers unavailable</h1><Link to="/dashboard">Back to dashboard</Link></main>

  return (
    <main className="dashboard">
      <h1>Blood donation</h1>
      <p>Find local blood donation centers and add new opportunities.</p>

      <form onSubmit={handleSubmit}>
        <label>
          Center name
          <input name="name" value={form.name} onChange={handleChange} required />
        </label>

        <label>
          Location
          <input name="location" value={form.location} onChange={handleChange} required />
        </label>

        <label>
          Contact
          <input name="contact" value={form.contact} onChange={handleChange} required />
        </label>

        <label>
          Description
          <textarea name="description" value={form.description} onChange={handleChange} required rows="3" />
        </label>

        <button type="submit" disabled={saveMutation.isPending}>
          {saveMutation.isPending ? 'Saving…' : editingId ? 'Update center' : 'Add center'}
        </button>

        {editingId && (
          <button type="button" onClick={() => { setEditingId(null); setForm(defaultForm) }}>
            Cancel
          </button>
        )}
      </form>

      <section>
        <h2>Available centers</h2>
        {data.length === 0 ? <p>No centers added yet.</p> : data.map((center) => (
          <article key={center.id}>
            <h3>{center.name}</h3>
            <p>{center.location}</p>
            <p>{center.contact}</p>
            <p>{center.description}</p>
            <button type="button" onClick={() => handleEdit(center)}>Edit</button>
            <button type="button" onClick={() => deleteMutation.mutate(center.id)}>Delete</button>
          </article>
        ))}
      </section>

      <p><Link to="/dashboard">Back to dashboard</Link></p>
    </main>
  )
}
