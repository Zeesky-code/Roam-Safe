// Geolocation functionality
document.addEventListener("DOMContentLoaded", function () {
  if ("geolocation" in navigator) {
    navigator.geolocation.getCurrentPosition(async (position) => {
      const lat = position.coords.latitude;
      const lon = position.coords.longitude;

      try {
        const res = await fetch(`https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json`);
        const data = await res.json();
        const city = data.address.city || data.address.town || data.address.village;

        if (city) {
          const input = document.querySelector("input[name='city']");
          if (input) input.value = city;
        }
      } catch (err) {
        console.error("Location lookup failed:", err);
      }
    }, (error) => {
      console.error("Geolocation error:", error);
    });
  }
});

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
        resultsDiv.innerHTML = `
          <h3>Found ${scams.length} scam(s) for ${city}:</h3>
          <div class="scams-grid">
            ${scams.map(s => `
              <div class="scam-card">
                <h4>${s.name}</h4>
                <p><strong>Description:</strong> ${s.description}</p>
                <p><strong>Prevention:</strong> ${s.prevention}</p>
                <p><strong>Category:</strong> ${s.categoryName || 'N/A'}</p>
              </div>
            `).join('')}
          </div>
        `;
      }
    } catch (e) {
      console.error('Error fetching scams:', e);
      resultsDiv.innerHTML = "<p>Failed to load scams.</p>";
    }
  }
