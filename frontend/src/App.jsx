import { useEffect, useState } from 'react'

function App() {
  const [status, setStatus] = useState('checking...')

  useEffect(() => {
    fetch('http://localhost:8080/api/health')
      .then((res) => res.json())
      .then((data) => setStatus(`${data.status} — ${data.service}`))
      .catch(() => setStatus('backend unreachable (is Spring Boot running?)'))
  }, [])

  return (
    <div style={{ fontFamily: 'sans-serif', padding: '2rem' }}>
      <h1>CodeMind</h1>
      <p>Backend status: <strong>{status}</strong></p>
    </div>
  )
}

export default App
