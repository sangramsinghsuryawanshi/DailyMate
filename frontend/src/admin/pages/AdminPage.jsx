import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getAdminComplaints, updateComplaintStatus } from '../services/adminApi'

const statusOptions = ['OPEN', 'IN_REVIEW', 'RESOLVED']

export default function AdminPage() {
  const queryClient = useQueryClient()

  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['admin-complaints'],
    queryFn: getAdminComplaints,
  })

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, status }) => updateComplaintStatus(id, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-complaints'] }),
  })

  if (isLoading) return <main className="dashboard"><h1>Loading admin dashboard…</h1></main>
  if (isError) return <main className="dashboard"><h1>Admin dashboard unavailable</h1><Link to="/dashboard">Back to dashboard</Link></main>

  return (
    <main className="dashboard">
      <h1>Community admin</h1>
      <p>Review resident reports and update their moderation status.</p>

      <section>
        {data.length === 0 ? <p>No complaints to review.</p> : data.map((complaint) => (
          <article key={complaint.id}>
            <h2>{complaint.title}</h2>
            <p><strong>{complaint.category}</strong> · {complaint.location}</p>
            <p>{complaint.description}</p>
            <p>Status: {complaint.status}</p>
            <label>
              Update status
              <select
                value={complaint.status}
                onChange={(event) => updateStatusMutation.mutate({ id: complaint.id, status: event.target.value })}
              >
                {statusOptions.map((status) => (
                  <option key={status} value={status}>{status}</option>
                ))}
              </select>
            </label>
          </article>
        ))}
      </section>

      <p><Link to="/dashboard">Back to dashboard</Link></p>
    </main>
  )
}
