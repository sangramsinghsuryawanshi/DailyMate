import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { createEmergencyContact, deleteEmergencyContact, getEmergencyContacts, updateEmergencyContact } from '../services/emergencyContactsApi'

const defaultForm = {
  name: '',
  category: '',
  phone: '',
  location: '',
  description: '',
}

export default function EmergencyContactsPage() {
  const queryClient = useQueryClient()
  const [form, setForm] = useState(defaultForm)
  const [editingId, setEditingId] = useState(null)

  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['emergency-contacts'],
    queryFn: getEmergencyContacts,
  })

  const saveMutation = useMutation({
    mutationFn: (payload) => editingId ? updateEmergencyContact(editingId, payload) : createEmergencyContact(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['emergency-contacts'] })
      setForm(defaultForm)
      setEditingId(null)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteEmergencyContact,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['emergency-contacts'] }),
  })

  function handleChange(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    saveMutation.mutate(form)
  }

  function handleEdit(contact) {
    setEditingId(contact.id)
    setForm({
      name: contact.name,
      category: contact.category,
      phone: contact.phone,
      location: contact.location,
      description: contact.description,
    })
  }

  if (isLoading) return <main className="dashboard"><h1>Loading emergency contacts…</h1></main>
  if (isError) return <main className="dashboard"><h1>Emergency contacts unavailable</h1><Link to="/dashboard">Back to dashboard</Link></main>

  return (
    <main className="dashboard">
      <h1>Emergency contacts</h1>
      <p>Keep local support contacts close at hand when every minute matters.</p>

      <form onSubmit={handleSubmit}>
        <label>
          Name
          <input name="name" value={form.name} onChange={handleChange} required />
        </label>

        <label>
          Category
          <input name="category" value={form.category} onChange={handleChange} required />
        </label>

        <label>
          Phone
          <input name="phone" value={form.phone} onChange={handleChange} required />
        </label>

        <label>
          Location
          <input name="location" value={form.location} onChange={handleChange} required />
        </label>

        <label>
          Description
          <textarea name="description" value={form.description} onChange={handleChange} required rows="3" />
        </label>

        <button type="submit" disabled={saveMutation.isPending}>
          {saveMutation.isPending ? 'Saving…' : editingId ? 'Update contact' : 'Add contact'}
        </button>

        {editingId && (
          <button type="button" onClick={() => { setEditingId(null); setForm(defaultForm) }}>
            Cancel
          </button>
        )}
      </form>

      <section>
        <h2>Available contacts</h2>
        {data.length === 0 ? <p>No emergency contacts added yet.</p> : data.map((contact) => (
          <article key={contact.id}>
            <h3>{contact.name}</h3>
            <p><strong>{contact.category}</strong></p>
            <p>{contact.phone}</p>
            <p>{contact.location}</p>
            <p>{contact.description}</p>
            <button type="button" onClick={() => handleEdit(contact)}>Edit</button>
            <button type="button" onClick={() => deleteMutation.mutate(contact.id)}>Delete</button>
          </article>
        ))}
      </section>

      <p><Link to="/dashboard">Back to dashboard</Link></p>
    </main>
  )
}
