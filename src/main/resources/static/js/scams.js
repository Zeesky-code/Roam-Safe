// Load page and setup event listeners
document.addEventListener("DOMContentLoaded", function () {
  // Add enter key support for search
  document.getElementById("city-input")?.addEventListener("keypress", function(e) {
    if (e.key === "Enter") {
      searchScams();
    }
  });
  
  // Add event listeners for filters
  document.getElementById("safety-zone-filter")?.addEventListener("change", applyFilters);
  document.getElementById("incident-type-filter")?.addEventListener("change", applyFilters);
  document.getElementById("night-time-filter")?.addEventListener("change", applyFilters);
});

// Simple search function that redirects to the server-side rendered page
function searchScams() {
    const city = document.getElementById("city-input").value.trim();
    if (!city) {
      alert("Please enter a city name first");
      return;
    }
    
    // Redirect to the server-side rendered scams page with the city parameter
    window.location.href = `/scams?city=${encodeURIComponent(city)}`;
}

// Alias for the button click
function fetchScams() {
    searchScams();
}

// Apply filters to the currently displayed results
function applyFilters() {
    const safetyZoneFilter = document.getElementById("safety-zone-filter").value;
    const incidentTypeFilter = document.getElementById("incident-type-filter").value;
    const nightTimeFilter = document.getElementById("night-time-filter").checked;
    
    const scamCards = document.querySelectorAll('.scam-card');
    
    scamCards.forEach(card => {
        let showCard = true;
        
        // Safety zone filter
        if (safetyZoneFilter) {
            const safetyBadge = card.querySelector('.safety-badge');
            if (!safetyBadge || !safetyBadge.textContent.includes(safetyZoneFilter)) {
                showCard = false;
            }
        }
        
        // Incident type filter
        if (incidentTypeFilter && showCard) {
            const typeElement = card.querySelector('p:contains("Type:")');
            if (!typeElement || !typeElement.textContent.includes(incidentTypeFilter)) {
                showCard = false;
            }
        }
        
        // Night time filter
        if (nightTimeFilter && showCard) {
            const nightTimeElement = card.querySelector('p:contains("Night Time")');
            if (!nightTimeElement) {
                showCard = false;
            }
        }
        
        // Show/hide the card
        card.style.display = showCard ? 'block' : 'none';
    });
    
    // Update the count
    const visibleCards = document.querySelectorAll('.scam-card[style="display: block"], .scam-card:not([style])');
    const resultsHeader = document.querySelector('.scams-grid h3');
    if (resultsHeader) {
        const city = document.getElementById("city-input").value.trim();
        resultsHeader.textContent = `Found ${visibleCards.length} safety report(s) for ${city}:`;
    }
}
