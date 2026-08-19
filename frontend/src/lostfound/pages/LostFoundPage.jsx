import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { createLostFoundPost, deleteLostFoundPost, getLostFoundPosts, updateLostFoundPost } from '../services/lostFoundApi'

const defaultForm = {
  title: '',
  itemType: '',
  location: '',
  description: '',
  contactName: '',
  contactPhone: '',
}

export default function LostFoundPage() {
  const queryClient = useQueryClient()
  const [form, setForm] = useState(defaultForm)
  const [editingId, setEditingId] = useState(null)

  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['lost-found-posts'],
    queryFn: getLostFoundPosts,
  })

  const saveMutation = useMutation({
    mutationFn: (payload) => editingId ? updateLostFoundPost(editingId, payload) : createLostFoundPost(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lost-found-posts'] })
      setForm(defaultForm)
      setEditingId(null)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteLostFoundPost,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['lost-found-posts'] }),
  })

  function handleChange(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    saveMutation.mutate(form)
  }

  function handleEdit(post) {
    setEditingId(post.id)
    setForm({
      title: post.title,
      itemType: post.itemType,
      location: post.location,
      description: post.description,
      contactName: post.contactName,
      contactPhone: post.contactPhone,
    })
  }

  if (isLoading) return <main className="dashboard"><h1>Loading lost and found posts…</h1></main>
  if (isError) return <main className="dashboard"><h1>Lost and found unavailable</h1><Link to="/dashboard">Back to dashboard</Link></main>

  return (
    <main className="dashboard">
      <h1>Lost &amp; found</h1>
      <p>Report a missing item or help reunite a local community item.</p>

      <form onSubmit={handleSubmit}>
        <label>
          Title
          <input name="title" value={form.title} onChange={handleChange} required />
        </label>

        <label>
          Item type
          <input name="itemType" value={form.itemType} onChange={handleChange} required />
        </label>

        <label>
          Location
          <input name="location" value={form.location} onChange={handleChange} required />
        </label>

        <label>
          Description
          <textarea name="description" value={form.description} onChange={handleChange} required rows="4" />
        </label>

        <label>
          Contact name
          <input name="contactName" value={form.contactName} onChange={handleChange} required />
        </label>

        <label>
          Contact phone
          <input name="contactPhone" value={form.contactPhone} onChange={handleChange} required />
        </label>

        <button type="submit" disabled={saveMutation.isPending}>
          {saveMutation.isPending ? 'Saving…' : editingId ? 'Update post' : 'Add post'}
        </button>

        {editingId && (
          <button type="button" onClick={() => { setEditingId(null); setForm(defaultForm) }}>
            Cancel
          </button>
        )}
      </form>

      <section>
        <h2>Local posts</h2>
        {data.length === 0 ? <p>No posts yet.</p> : data.map((post) => (
          <article key={post.id}>
            <h3>{post.title}</h3>
            <p>{post.itemType} · {post.location}</p>
            <p>{post.description}</p>
            <p>Contact: {post.contactName} ({post.contactPhone})</p>
            <button type="button" onClick={() => handleEdit(post)}>Edit</button>
            <button type="button" onClick={() => deleteMutation.mutate(post.id)}>Delete</button>
          </article>
        ))}
      </section>

      <p><Link to="/dashboard">Back to dashboard</Link></p>
    </main>
  )
}
