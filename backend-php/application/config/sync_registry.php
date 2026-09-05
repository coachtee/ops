<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * Registers every syncable model — mirrors backend/sync/registry.py's
 * registry.register("model_key", Model, Serializer) call in the Django
 * backend this replaces. Adding a new resource to sync push/pull is meant
 * to stay a one-line change here (plus its own Model class), same promise
 * the Django version's registry doc comment made.
 *
 * Order matters: MODEL_APPLY_ORDER in backend/sync/services.py applies
 * changes within one push batch in a fixed dependency order so a child
 * listed before its not-yet-applied parent in the same batch still
 * resolves correctly (see docs/API_CONTRACT.md's "Sync" section). Only
 * 'customer' is ported so far — see backend-php/README.md for what's
 * still pending.
 */
$config['sync_registry'] = array(
	'customer' => 'Customer_model',
);
