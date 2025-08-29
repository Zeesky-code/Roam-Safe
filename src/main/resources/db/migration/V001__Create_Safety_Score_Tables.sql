-- Create safety_scores table
CREATE TABLE IF NOT EXISTS safety_scores (
    id BIGSERIAL PRIMARY KEY,
    city_id BIGINT NOT NULL,
    overall_score INTEGER NOT NULL CHECK (overall_score >= 1 AND overall_score <= 100),
    financial_risk_score INTEGER NOT NULL CHECK (financial_risk_score >= 1 AND financial_risk_score <= 100),
    physical_risk_score INTEGER NOT NULL CHECK (physical_risk_score >= 1 AND physical_risk_score <= 100),
    digital_risk_score INTEGER NOT NULL CHECK (digital_risk_score >= 1 AND digital_risk_score <= 100),
    total_scam_count INTEGER NOT NULL CHECK (total_scam_count >= 0),
    recent_scam_count INTEGER NOT NULL CHECK (recent_scam_count >= 0),
    confidence_level DOUBLE PRECISION NOT NULL CHECK (confidence_level >= 0.0 AND confidence_level <= 1.0),
    top_scam_types TEXT,
    ai_insights TEXT,
    last_calculated TIMESTAMP NOT NULL,
    last_updated TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_safety_scores_city FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE CASCADE,
    CONSTRAINT uk_safety_scores_city UNIQUE (city_id)
);

-- Create safety_score_history table
CREATE TABLE IF NOT EXISTS safety_score_history (
    id BIGSERIAL PRIMARY KEY,
    city_id BIGINT NOT NULL,
    previous_score INTEGER,
    new_score INTEGER NOT NULL,
    previous_financial_score INTEGER,
    new_financial_score INTEGER NOT NULL,
    previous_physical_score INTEGER,
    new_physical_score INTEGER NOT NULL,
    previous_digital_score INTEGER,
    new_digital_score INTEGER NOT NULL,
    change_reason TEXT,
    calculated_at TIMESTAMP NOT NULL,
    scam_count_change INTEGER,
    confidence_level_change DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_safety_score_history_city FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_safety_scores_city_id ON safety_scores(city_id);
CREATE INDEX IF NOT EXISTS idx_safety_scores_overall_score ON safety_scores(overall_score);
CREATE INDEX IF NOT EXISTS idx_safety_scores_last_calculated ON safety_scores(last_calculated);
CREATE INDEX IF NOT EXISTS idx_safety_scores_confidence_level ON safety_scores(confidence_level);
CREATE INDEX IF NOT EXISTS idx_safety_scores_financial_risk ON safety_scores(financial_risk_score);
CREATE INDEX IF NOT EXISTS idx_safety_scores_physical_risk ON safety_scores(physical_risk_score);
CREATE INDEX IF NOT EXISTS idx_safety_scores_digital_risk ON safety_scores(digital_risk_score);

CREATE INDEX IF NOT EXISTS idx_safety_score_history_city_id ON safety_score_history(city_id);
CREATE INDEX IF NOT EXISTS idx_safety_score_history_calculated_at ON safety_score_history(calculated_at);
CREATE INDEX IF NOT EXISTS idx_safety_score_history_score_change ON safety_score_history((new_score - COALESCE(previous_score, 0)));

-- Create a function to automatically update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$$ language 'plpgsql';

-- Create trigger to automatically update updated_at on safety_scores
CREATE TRIGGER update_safety_scores_updated_at 
    BEFORE UPDATE ON safety_scores 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE safety_scores IS 'Stores calculated safety scores for cities based on scam data analysis';
COMMENT ON TABLE safety_score_history IS 'Audit trail for safety score changes over time';

COMMENT ON COLUMN safety_scores.overall_score IS 'Overall safety score from 1-100 (higher is safer)';
COMMENT ON COLUMN safety_scores.financial_risk_score IS 'Financial risk score from 1-100 (higher is safer)';
COMMENT ON COLUMN safety_scores.physical_risk_score IS 'Physical risk score from 1-100 (higher is safer)';
COMMENT ON COLUMN safety_scores.digital_risk_score IS 'Digital risk score from 1-100 (higher is safer)';
COMMENT ON COLUMN safety_scores.total_scam_count IS 'Total number of scams reported for this city';
COMMENT ON COLUMN safety_scores.recent_scam_count IS 'Number of scams reported in the last 6 months';
COMMENT ON COLUMN safety_scores.confidence_level IS 'Confidence level of the score calculation (0.0-1.0)';
COMMENT ON COLUMN safety_scores.top_scam_types IS 'JSON array of the top 3 most common scam types';
COMMENT ON COLUMN safety_scores.ai_insights IS 'AI-generated insights for premium users';
COMMENT ON COLUMN safety_scores.last_calculated IS 'When the score was last calculated';
COMMENT ON COLUMN safety_scores.last_updated IS 'When the record was last updated';

COMMENT ON COLUMN safety_score_history.change_reason IS 'Reason for the score change (e.g., new scam data, algorithm update)';
COMMENT ON COLUMN safety_score_history.scam_count_change IS 'Change in scam count since last calculation';
COMMENT ON COLUMN safety_score_history.confidence_level_change IS 'Change in confidence level since last calculation';