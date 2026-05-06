"""
Log parsing utility functions for E2E tests
"""
import re
import logging
from typing import List, Dict, Optional, Any

logger = logging.getLogger(__name__)


def parse_optimizer_logs(logs: str) -> Dict[str, Any]:
    """Parse optimizer logs to extract key information"""
    result = {
        'initialized': False,
        'profiles_installed': {
            'metric': False,
            'metadata': False,
            'layers': False
        },
        'jobs_triggered': 0,
        'webhooks_received': 0,
        'errors': []
    }
    
    # Check for initialization
    if 'Bulk scheduler initialized' in logs or 'INFO_BULK_SCHEDULER_INITIALIZED' in logs:
        result['initialized'] = True
    
    # Check for profile installation
    if 'Installing metric profile' in logs or 'Metric profile installed' in logs:
        result['profiles_installed']['metric'] = True
    
    if 'Installing metadata profile' in logs or 'Metadata profile installed' in logs:
        result['profiles_installed']['metadata'] = True
    
    if 'Installing layer' in logs or 'Layer installed' in logs:
        result['profiles_installed']['layers'] = True
    
    # Count job triggers
    job_trigger_pattern = r'Starting scheduled bulk API call|Calling bulk API'
    result['jobs_triggered'] = len(re.findall(job_trigger_pattern, logs, re.IGNORECASE))
    
    # Count webhook callbacks
    webhook_pattern = r'Received webhook|Processing webhook'
    result['webhooks_received'] = len(re.findall(webhook_pattern, logs, re.IGNORECASE))
    
    # Extract errors
    error_lines = [line for line in logs.split('\n') if 'ERROR' in line or 'Exception' in line]
    result['errors'] = error_lines[:10]  # Limit to first 10 errors
    
    return result


def check_log_for_message(logs: str, message: str, case_sensitive: bool = False) -> bool:
    """Check if logs contain a specific message"""
    if case_sensitive:
        return message in logs
    else:
        return message.lower() in logs.lower()


def extract_job_ids(logs: str) -> List[str]:
    """Extract job IDs from logs"""
    # Pattern to match job IDs like "job-123", "job-abc-456", etc.
    pattern = r'job[-_][a-zA-Z0-9\-]+'
    job_ids = re.findall(pattern, logs)
    return list(set(job_ids))  # Return unique job IDs


def extract_job_ids_from_logs(logs: str) -> List[Dict[str, str]]:
    """
    Extract job IDs and their completion status from logs
    Returns list of dicts with job_id, status, total, processed, existing
    """
    job_info = []
    
    # Pattern for "Bulk API call successful. Response: {"job_id":"<uuid>"}"
    job_trigger_pattern = r'Bulk API call successful\. Response: \{"job_id":"([a-f0-9\-]+)"\}'
    triggered_jobs = re.findall(job_trigger_pattern, logs)
    
    # Pattern for "Job <uuid> completed. Total: X, Processed: Y, Existing: Z"
    job_complete_pattern = r'Job ([a-f0-9\-]+) completed\. Total: (\d+), Processed: (\d+), Existing: (\d+)'
    completed_jobs = re.findall(job_complete_pattern, logs)
    
    # Build job info from completed jobs
    for job_id, total, processed, existing in completed_jobs:
        job_info.append({
            'job_id': job_id,
            'status': 'completed',
            'total': int(total),
            'processed': int(processed),
            'existing': int(existing)
        })
    
    # Add triggered but not yet completed jobs
    completed_ids = {job['job_id'] for job in job_info}
    for job_id in triggered_jobs:
        if job_id not in completed_ids:
            job_info.append({
                'job_id': job_id,
                'status': 'triggered',
                'total': None,
                'processed': None,
                'existing': None
            })
    
    return job_info


def verify_profile_installation_logs(logs: str, expected_profiles: Dict[str, List]) -> Dict[str, bool]:
    """
    Verify that profile installation messages exist in logs
    
    Args:
        logs: The log content to search
        expected_profiles: Dict with keys 'metadata_profiles', 'metric_profiles', 'layers'
                          Each containing list of profile names or dicts with 'name' key
    
    Returns:
        Dict with profile names as keys and boolean values indicating if found in logs
    """
    results = {}
    
    # Check metadata profiles
    if 'metadata_profiles' in expected_profiles:
        for profile in expected_profiles['metadata_profiles']:
            profile_name = profile['name'] if isinstance(profile, dict) else profile
            log_message = f"Metadata profile: Installed: {profile_name}"
            results[f"metadata:{profile_name}"] = check_log_for_message(logs, log_message)
    
    # Check metric profiles
    if 'metric_profiles' in expected_profiles:
        for profile in expected_profiles['metric_profiles']:
            profile_name = profile['name'] if isinstance(profile, dict) else profile
            log_message = f"Metric profile: Installed: {profile_name}"
            results[f"metric:{profile_name}"] = check_log_for_message(logs, log_message)
    
    # Check layers
    if 'layers' in expected_profiles:
        for layer in expected_profiles['layers']:
            layer_name = layer if isinstance(layer, str) else layer['name']
            log_message = f"Layer: Installed: {layer_name}"
            results[f"layer:{layer_name}"] = check_log_for_message(logs, log_message)
    
    return results


def check_bulk_job_with_autotune_label(logs: str) -> bool:
    """
    Check if bulk API call includes autotune label filter
    Returns True if found
    """
    # Pattern to match the autotune label in bulk API payload
    pattern = r'"kruize/autotune"\s*:\s*"enabled"'
    return bool(re.search(pattern, logs))


def count_log_occurrences(logs: str, pattern: str) -> int:
    """Count occurrences of a pattern in logs"""
    return len(re.findall(pattern, logs, re.IGNORECASE))


def get_log_lines_with_pattern(logs: str, pattern: str, context_lines: int = 2) -> List[str]:
    """Get log lines matching a pattern with context"""
    lines = logs.split('\n')
    matching_lines = []
    
    for i, line in enumerate(lines):
        if re.search(pattern, line, re.IGNORECASE):
            # Get context lines before and after
            start = max(0, i - context_lines)
            end = min(len(lines), i + context_lines + 1)
            context = lines[start:end]
            matching_lines.extend(context)
            matching_lines.append('---')  # Separator
    
    return matching_lines


def verify_no_errors(logs: str, allowed_errors: Optional[List[str]] = None) -> tuple[bool, List[str]]:
    """Verify that logs don't contain unexpected errors"""
    allowed_errors = allowed_errors or []
    
    error_lines = [line for line in logs.split('\n') if 'ERROR' in line or 'Exception' in line]
    
    # Filter out allowed errors
    unexpected_errors = []
    for error_line in error_lines:
        is_allowed = any(allowed in error_line for allowed in allowed_errors)
        if not is_allowed:
            unexpected_errors.append(error_line)
    
    return len(unexpected_errors) == 0, unexpected_errors


def extract_api_calls(logs: str) -> Dict[str, int]:
    """Extract and count API calls from logs"""
    api_calls = {
        'bulk_api': 0,
        'list_datasources': 0,
        'list_metric_profiles': 0,
        'list_metadata_profiles': 0,
        'list_layers': 0
    }
    
    # Count bulk API calls
    api_calls['bulk_api'] = count_log_occurrences(logs, r'Calling bulk API|bulkCreateExperiments')
    
    # Count profile API calls
    api_calls['list_datasources'] = count_log_occurrences(logs, r'listDatasources|/datasources')
    api_calls['list_metric_profiles'] = count_log_occurrences(logs, r'listMetricProfiles|/listMetricProfiles')
    api_calls['list_metadata_profiles'] = count_log_occurrences(logs, r'listMetadataProfiles|/listMetadataProfiles')
    api_calls['list_layers'] = count_log_occurrences(logs, r'listLayers|/listLayers')
    
    return api_calls


def save_logs_to_file(logs: str, filename: str):
    """Save logs to a file"""
    try:
        with open(filename, 'w') as f:
            f.write(logs)
        logger.info(f"Logs saved to {filename}")
    except Exception as e:
        logger.error(f"Failed to save logs to {filename}: {e}")

# Made with Bob
