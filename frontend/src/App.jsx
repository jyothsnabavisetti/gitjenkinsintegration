import React, { useState } from 'react'

export default function App() {
  const [file, setFile] = useState(null)
  const [plant, setPlant] = useState('')
  const [png, setPng] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const submit = async (e) => {
    e.preventDefault()
    if (!file) return
    setLoading(true)
    setError('')
    const form = new FormData()
    form.append('file', file)
    try {
      const res = await fetch('/api/upload', { method: 'POST', body: form })
      if (!res.ok) throw new Error('Upload failed: ' + res.statusText)
      const data = await res.json()
      setPlant(data.plantuml)
      setPng('data:image/png;base64,' + data.pngBase64)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="container">
      <h1>Seq Diagram Generator</h1>
      <form onSubmit={submit}>
        <input type="file" accept=".zip" onChange={(e) => setFile(e.target.files[0])} />
        <button type="submit" disabled={loading}>Upload</button>
      </form>

      {error && <div className="error">{error}</div>}
      {loading && <div>Uploading...</div>}

      {png && (
        <div>
          <h2>Diagram</h2>
          <img src={png} alt="Sequence diagram" />
        </div>
      )}

      {plant && (
        <div>
          <h2>PlantUML</h2>
          <textarea readOnly value={plant} rows={10} />
        </div>
      )}
    </div>
  )
}