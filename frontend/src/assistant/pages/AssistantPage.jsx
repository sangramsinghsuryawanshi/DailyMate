import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Button from '../../components/Button'
import Input from '../../components/Input'
import MainLayout from '../../layouts/MainLayout'
import { createAssistantConversation, deleteAssistantConversation, getAssistantConversations, updateAssistantConversation } from '../services/assistantApi'

const defaultForm = {
  title: '',
  prompt: '',
  response: '',
}

export default function AssistantPage() {
  const queryClient = useQueryClient()
  const [form, setForm] = useState(defaultForm)
  const [editingId, setEditingId] = useState(null)

  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['assistant-conversations'],
    queryFn: getAssistantConversations,
  })

  const promptCount = useMemo(() => data.length, [data])

  const saveMutation = useMutation({
    mutationFn: (payload) => editingId ? updateAssistantConversation(editingId, payload) : createAssistantConversation(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['assistant-conversations'] })
      setForm(defaultForm)
      setEditingId(null)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteAssistantConversation,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['assistant-conversations'] }),
  })

  function handleChange(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    saveMutation.mutate(form)
  }

  function handleEdit(conversation) {
    setEditingId(conversation.id)
    setForm({
      title: conversation.title,
      prompt: conversation.prompt,
      response: conversation.response,
    })
  }

  if (isLoading) return (
    <MainLayout>
      <main className="page-state"><h1>Loading assistant…</h1></main>
    </MainLayout>
  )

  if (isError) return (
    <MainLayout>
      <main className="page-state"><h1>Assistant unavailable</h1><Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link></main>
    </MainLayout>
  )

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Smart support</p>
          <h1>DailyMate AI assistant</h1>
          <p className="subtle-text">Capture a quick prompt and save a helpful personalized response for later.</p>
        </div>
        <Link to="/dashboard" className="btn btn-ghost">Back</Link>
      </section>

      <section className="assistant-grid">
        <div className="panel">
          <div className="panel-header">
            <h2>{editingId ? 'Edit prompt' : 'New prompt'}</h2>
          </div>

          <form className="notification-form" onSubmit={handleSubmit}>
            <Input label="Title" name="title" value={form.title} onChange={handleChange} required />

            <label className="field">
              <span>Prompt</span>
              <textarea name="prompt" value={form.prompt} onChange={handleChange} required rows="4" />
            </label>

            <label className="field">
              <span>Response</span>
              <textarea name="response" value={form.response} onChange={handleChange} required rows="4" />
            </label>

            <div className="profile-actions">
              <Button type="submit" disabled={saveMutation.isPending}>{saveMutation.isPending ? 'Saving…' : editingId ? 'Update entry' : 'Save response'}</Button>
              {editingId && <Button variant="ghost" onClick={() => { setEditingId(null); setForm(defaultForm) }}>Cancel</Button>}
            </div>
          </form>
        </div>

        <aside className="panel summary-panel">
          <div className="panel-header">
            <h2>Memory</h2>
          </div>
          <div className="detail-list">
            <li><strong>Saved prompts:</strong> {promptCount}</li>
            <li><strong>Focus:</strong> Personal routines</li>
            <li><strong>Tips:</strong> Keep prompts specific</li>
          </div>

          <div className="notification-list" style={{ marginTop: '1rem' }}>
            {data.length === 0 ? (
              <div className="empty-state">
                <h3>No saved prompts yet</h3>
                <p className="muted">Your useful prompts will appear here.</p>
              </div>
            ) : data.map((conversation) => (
              <article key={conversation.id} className="notification-item">
                <div className="notification-head">
                  <span className="notification-badge reminder">Saved</span>
                  <span className="small-muted">Prompt</span>
                </div>
                <h3>{conversation.title}</h3>
                <p><strong>Prompt:</strong> {conversation.prompt}</p>
                <p><strong>Response:</strong> {conversation.response}</p>
                <div className="notification-actions">
                  <Button variant="secondary" onClick={() => handleEdit(conversation)}>Edit</Button>
                  <Button variant="ghost" onClick={() => deleteMutation.mutate(conversation.id)}>Delete</Button>
                </div>
              </article>
            ))}
          </div>
        </aside>
      </section>
    </MainLayout>
  )
}
