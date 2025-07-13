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
  
  // Add event listeners for filters
  document.getElementById("safety-zone-filter")?.addEventListener("change", applyFilters);
  document.getElementById("incident-type-filter")?.addEventListener("change", applyFilters);
  document.getElementById("night-time-filter")?.addEventListener("change", applyFilters);
});

// Helper functions for safety zone display
function getSafetyZoneEmoji(zone) {
  const emojis = {
    'GREEN': '🟢',
    'YELLOW': '🟡', 
    'RED': '🔴',
    'UNKNOWN': '⚪'
  };
  return emojis[zone] || '⚪';
}

function getIncidentTypeEmoji(type) {
  const emojis = {
    'SCAM': '🚨',
    'HARASSMENT': '⚠️',
    'THEFT': '💰',
    'POSITIVE': '✅'
  };
  return emojis[type] || '📝';
}

let allScams = []; // Store all scams for filtering

async function fetchScams() {
    const city = document.getElementById("city-input").value.trim();
    const resultsDiv = document.getElementById("scam-results");
    resultsDiv.innerHTML = "Loading...";
  
    try {
      const res = await fetch(`/scams/api?city=${encodeURIComponent(city)}`);
      allScams = await res.json();
      
      displayScams(allScams, city);
    } catch (e) {
      console.error('Error fetching scams:', e);
      resultsDiv.innerHTML = "<p>Failed to load scams.</p>";
    }
}

function displayScams(scams, city) {
    const resultsDiv = document.getElementById("scam-results");
    
    if (scams.length === 0) {
      resultsDiv.innerHTML = "<p>No safety reports found for this city.</p>";
    } else {
      resultsDiv.innerHTML = `
        <h3>Found ${scams.length} safety report(s) for ${city}:</h3>
        <div class="scams-grid">
          ${scams.map(s => `
            <div class="scam-card ${s.safetyZone ? s.safetyZone.toLowerCase() : ''}">
              <div class="card-header">
                <h4>${s.name}</h4>
                ${s.safetyZone ? `<span class="safety-badge ${s.safetyZone.toLowerCase()}">${getSafetyZoneEmoji(s.safetyZone)} ${s.safetyZone}</span>` : ''}
              </div>
              <p><strong>Description:</strong> ${s.description}</p>
              ${s.neighborhood ? `<p><strong>Area:</strong> ${s.neighborhood}</p>` : ''}
              ${s.incidentType ? `<p><strong>Type:</strong> ${getIncidentTypeEmoji(s.incidentType)} ${s.incidentType}</p>` : ''}
              ${s.safetyRating ? `<p><strong>Safety Rating:</strong> ${'⭐'.repeat(s.safetyRating)} (${s.safetyRating}/5)</p>` : ''}
              ${s.isNightTimeIncident ? `<p><strong>⚠️ Night Time Incident</strong></p>` : ''}
              <p><strong>Prevention:</strong> ${s.prevention}</p>
              <p><strong>Category:</strong> ${s.categoryName || 'N/A'}</p>
            </div>
          `).join('')}
        </div>
      `;
    }
}

function applyFilters() {
    const safetyZoneFilter = document.getElementById("safety-zone-filter").value;
    const incidentTypeFilter = document.getElementById("incident-type-filter").value;
    const nightTimeFilter = document.getElementById("night-time-filter").checked;
    const city = document.getElementById("city-input").value.trim();
    
    let filteredScams = allScams.filter(scam => {
        // Safety zone filter
        if (safetyZoneFilter && scam.safetyZone !== safetyZoneFilter) {
            return false;
        }
        
        // Incident type filter
        if (incidentTypeFilter && scam.incidentType !== incidentTypeFilter) {
            return false;
        }
        
        // Night time filter
        if (nightTimeFilter && !scam.isNightTimeIncident) {
            return false;
        }
        
        return true;
    });
    
    displayScams(filteredScams, city);
}
