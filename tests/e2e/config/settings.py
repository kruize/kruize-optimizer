"""Configuration settings for e2e tests.

This module contains all configuration constants and defaults matching
the bash demo scripts (optimizer_demo.sh, common.sh, common_helper.sh).
"""

import os
from dataclasses import dataclass, field
from typing import Optional


@dataclass
class Config:
    """Configuration for e2e test framework."""

    # Cluster configuration
    cluster_type: str = "kind"  # kind | minikube | openshift
    install_mode: str = "manifest"  # manifest | operator
    env_setup: bool = False  # -f flag equivalent
    app_namespace: str = "default"

    # Custom images
    kruize_image: Optional[str] = None
    kruize_ui_image: Optional[str] = None
    kruize_operator_image: Optional[str] = None
    kruize_optimizer_image: Optional[str] = None

    # Test control
    skip_setup: bool = False
    skip_teardown: bool = False
    terminate: bool = False  # -t flag: cleanup and exit
    working_dir: Optional[str] = None
    optimizer_wait_duration: Optional[int] = None

    # Derived/constant values
    NAMESPACE_KIND: str = field(default="monitoring", init=False)
    NAMESPACE_OPENSHIFT: str = field(default="openshift-tuning", init=False)
    KIND_IP: str = field(default="127.0.0.1", init=False)
    KIND_KUBERNETES_VERSION: str = field(default="v1.28.0", init=False)

    KRUIZE_PORT: int = field(default=8080, init=False)
    KRUIZE_UI_PORT: int = field(default=8081, init=False)
    TECHEMPOWER_PORT: int = field(default=8082, init=False)

    TARGET: str = field(default="crc", init=False)

    # Image defaults
    KRUIZE_DOCKER_REPO: str = field(default="quay.io/kruize/autotune_operator", init=False)
    KRUIZE_OPERATOR_DOCKER_REPO: str = field(default="quay.io/kruize/kruize-operator", init=False)
    KRUIZE_OPTIMIZER_IMAGE_DEFAULT: str = field(default="quay.io/kruize/kruize-optimizer:0.0.1", init=False)

    # Wait times
    OPTIMIZER_WAIT_DURATION_DEFAULT: int = field(default=120, init=False)

    # Repository URLs
    AUTOTUNE_REPO: str = field(default="https://github.com/kruize/autotune.git", init=False)
    AUTOTUNE_BRANCH: str = field(default="mvp_demo", init=False)

    BENCHMARKS_REPO: str = field(default="https://github.com/kruize/benchmarks.git", init=False)

    KRUIZE_OPERATOR_REPO: str = field(default="https://github.com/kruize/kruize-operator.git", init=False)
    KRUIZE_OPERATOR_BRANCH: str = field(default="mvp_demo", init=False)

    KRUIZE_OPTIMIZER_REPO: str = field(default="https://github.com/kruize/kruize-optimizer.git", init=False)
    KRUIZE_OPTIMIZER_BRANCH: str = field(default="mvp_demo", init=False)

    @property
    def namespace(self) -> str:
        """Return the appropriate namespace based on cluster type."""
        if self.cluster_type == "openshift":
            return self.NAMESPACE_OPENSHIFT
        return self.NAMESPACE_KIND

    @property
    def kubectl_cmd(self) -> str:
        """Return the appropriate kubectl command based on cluster type."""
        if self.cluster_type == "openshift":
            return "oc"
        return "kubectl"

    @property
    def cluster_type_for_deploy(self) -> str:
        """Return the cluster type to pass to deploy.sh (kind uses minikube)."""
        if self.cluster_type == "kind":
            return "minikube"
        return self.cluster_type

    @property
    def kruize_optimizer_image_resolved(self) -> str:
        """Return the kruize-optimizer image, using custom or default."""
        return self.kruize_optimizer_image or self.KRUIZE_OPTIMIZER_IMAGE_DEFAULT

    @property
    def optimizer_wait_duration_resolved(self) -> int:
        """Return the optimizer wait duration, using custom or default."""
        if self.optimizer_wait_duration is not None:
            return self.optimizer_wait_duration
        # Check environment variable
        env_val = os.getenv("OPTIMIZER_WAIT_DURATION")
        if env_val:
            return int(env_val)
        return self.OPTIMIZER_WAIT_DURATION_DEFAULT

    @property
    def kruize_url(self) -> str:
        """Return the Kruize URL based on cluster type."""
        if self.cluster_type == "kind":
            return f"{self.KIND_IP}:{self.KRUIZE_PORT}"
        # For openshift, this will be set dynamically by getting the route
        return f"{self.KIND_IP}:{self.KRUIZE_PORT}"

    @classmethod
    def from_pytest_config(cls, pytest_config) -> "Config":
        """Create Config from pytest config (CLI args + env vars)."""
        return cls(
            cluster_type=pytest_config.getoption("--cluster-type"),
            install_mode=pytest_config.getoption("--install-mode"),
            env_setup=pytest_config.getoption("--env-setup"),
            app_namespace=pytest_config.getoption("--app-namespace"),
            kruize_image=pytest_config.getoption("--kruize-image"),
            kruize_ui_image=pytest_config.getoption("--kruize-ui-image"),
            kruize_operator_image=pytest_config.getoption("--kruize-operator-image"),
            kruize_optimizer_image=pytest_config.getoption("--kruize-optimizer-image"),
            skip_setup=pytest_config.getoption("--skip-setup"),
            skip_teardown=pytest_config.getoption("--skip-teardown"),
            terminate=pytest_config.getoption("--terminate"),
            working_dir=pytest_config.getoption("--working-dir"),
            optimizer_wait_duration=pytest_config.getoption("--optimizer-wait-duration"),
        )
