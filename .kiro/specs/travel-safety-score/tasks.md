# Implementation Plan

- [x] 1. Create core data models and database schema
  - Create SafetyScore entity with all required fields and relationships
  - Create SafetyScoreHistory entity for audit trail
  - Add database migration scripts for new tables
  - Create repository interfaces for data access
  - _Requirements: 1.1, 7.2_

- [ ] 2. Implement basic score calculation engine
  - Create ScoreCalculationService with core algorithm logic
  - Implement calculateScoreForCity method with frequency, severity, and recency weights
  - Add confidence level calculation based on data quality
  - Create unit tests for calculation logic with various scam data scenarios
  - _Requirements: 1.5, 7.1_

- [ ] 3. Build SafetyScoreService for score management
  - Implement getScoreForCity method with caching logic
  - Add isScoreStale method to check if recalculation is needed
  - Create getScoresForCities method for batch retrieval
  - Write integration tests for service layer operations
  - _Requirements: 1.1, 1.2_

- [ ] 4. Create REST API endpoints for basic score access
  - Implement SafetyScoreController with getScoreForCity endpoint
  - Add proper error handling for missing cities and calculation failures
  - Implement freemium logic to show basic scores to all users
  - Create API response DTOs for score data
  - Write controller tests for all endpoints
  - _Requirements: 1.1, 1.2, 6.1, 6.2_

- [ ] 5. Implement premium subscription validation system
  - Add subscription status fields to User model
  - Create PremiumFeaturesService with hasValidPremiumSubscription method
  - Implement subscription check middleware for premium endpoints
  - Add upgrade prompts for free users accessing premium features
  - Write tests for subscription validation logic
  - _Requirements: 6.2, 6.3, 6.4_

- [ ] 6. Build AI-powered insights for premium users
  - Integrate Gemini API service for generating detailed risk analysis
  - Implement generateAIInsights method in PremiumFeaturesService
  - Add top scam types identification and prevention recommendations
  - Create fallback logic when AI service is unavailable
  - Write tests with mocked AI responses
  - _Requirements: 2.2, 2.3, 2.5_

- [ ] 7. Create premium comparison feature
  - Implement compareScores endpoint for side-by-side city comparison
  - Add SafetyScoreComparison DTO with relative risk analysis
  - Limit comparisons to maximum 5 cities with proper validation
  - Generate tie-breaking insights for similar scores
  - Write tests for comparison logic and limits
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 8. Add score calculation transparency features
  - Create methodology explanation endpoint and UI component
  - Add data freshness indicators and confidence level display
  - Implement color-coded score indicators (green/yellow/red)
  - Show sample size and data quality warnings
  - Write tests for transparency features
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 9. Implement automated score updates and batch processing
  - Create scheduled task for recalculating all city scores
  - Add trigger for score recalculation when new scam data is added
  - Implement SafetyScoreHistory logging for audit trail
  - Add error handling and recovery for failed calculations
  - Write tests for batch processing and error scenarios
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 10. Build premium alert system for destination monitoring
  - Create SafetyAlert model and repository
  - Implement user watchlist functionality for tracking destinations
  - Add notification service for new high-risk scam alerts
  - Create alert prioritization by severity and user preferences
  - Write tests for alert generation and delivery
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 11. Add caching layer for performance optimization
  - Implement Redis caching for frequently accessed safety scores
  - Add cache invalidation logic when scores are updated
  - Create cache warming strategy for popular destinations
  - Add cache hit rate monitoring and performance metrics
  - Write tests for cache behavior and invalidation
  - _Requirements: 7.5_

- [ ] 12. Create frontend UI components for safety scores
  - Build safety score display component with color coding
  - Create premium comparison interface with side-by-side layout
  - Add upgrade prompts and subscription management UI
  - Implement responsive design for mobile users
  - Write frontend tests for all UI components
  - _Requirements: 1.3, 3.2, 6.2_

- [ ] 13. Integrate safety scores into existing scam search page
  - Modify ScamController to include safety scores in city search results
  - Update scams.html template to display safety scores prominently
  - Add premium feature teasers for free users
  - Ensure backward compatibility with existing functionality
  - Write integration tests for updated scam search flow
  - _Requirements: 1.1, 6.1_

- [ ] 14. Add comprehensive error handling and user feedback
  - Implement graceful degradation when scores are unavailable
  - Add user-friendly error messages for all failure scenarios
  - Create fallback content for cities with insufficient data
  - Add loading states and progress indicators for score calculations
  - Write tests for all error handling scenarios
  - _Requirements: 1.2, 4.4_

- [ ] 15. Create admin dashboard for score monitoring and management
  - Build admin interface for viewing score calculation status
  - Add manual score recalculation triggers for administrators
  - Create score accuracy monitoring and user feedback collection
  - Implement score override functionality for special cases
  - Write tests for admin functionality
  - _Requirements: 7.2, 7.4_