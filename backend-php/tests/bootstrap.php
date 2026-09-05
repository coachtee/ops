<?php
/**
 * These are HTTP-level integration tests, not unit tests of CI3 classes in
 * isolation — CodeIgniter 3 (like the Perfex CRM stack it mirrors) was
 * never built with dependency injection or unit-testability in mind, so
 * the standard, reliable way to test a CI3 API for real is to actually
 * call it over HTTP, the same way Android does. Requires a running
 * instance (see README.md's "Test" section) at OPS_TEST_BASE_URL
 * (default http://127.0.0.1:8080), backed by a real MySQL/MariaDB
 * database — same principle as the Django backend's APITestCase, which
 * also exercises real request/response cycles rather than mocking them.
 */
require __DIR__.'/../vendor/autoload.php';
require __DIR__.'/ApiTestCase.php';
