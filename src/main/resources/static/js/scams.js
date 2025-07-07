async function fetchScams() {
    const city = document.getElementById("city-input").value.trim();
    const resultsDiv = document.getElementById("scam-results");
    resultsDiv.innerHTML = "Loading...";
  
    try {
      const res = await fetch(`/scams/api?city=${encodeURIComponent(city)}`);
      const scams = await res.json();
  
      if (scams.length === 0) {
        resultsDiv.innerHTML = "<p>No scams found for this city.</p>";
      } else {
        resultsDiv.innerHTML = "<ul>" + scams.map(s => `
          <div class="scam-card">
            <h4>${s.name}</h4>
            <p><strong>Description:</strong> ${s.description}</p>
            <p><strong>Prevention:</strong> ${s.prevention}</p>
            <p><strong>Category:</strong> ${s.category ? s.category.name : 'N/A'}</p>
          </div>
        `).join('')  + "</ul>";
      }
    } catch (e) {
      console.error('Error fetching scams:', e);
      resultsDiv.innerHTML = "<p>Failed to load scams.</p>";
    }
  }
