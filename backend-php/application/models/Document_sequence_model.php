<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * MySQL equivalent of backend/common/models.py's DocumentSequence +
 * next_document_number(), which uses Django's select_for_update()
 * pessimistic row lock inside transaction.atomic(). This uses the direct
 * InnoDB equivalent: SELECT ... FOR UPDATE to lock the row (or, for a
 * business/doc_type pair's very first number, the gap where it would be)
 * before incrementing — always called from inside Sync::push_post()'s
 * already-open transaction, so the lock is held until that transaction
 * commits, exactly matching Django's atomicity guarantee.
 *
 * (An earlier version of this tried the classic MySQL
 * "INSERT ... ON DUPLICATE KEY UPDATE last_number = LAST_INSERT_ID(last_number + 1)"
 * atomic-counter idiom, which is elegant when the counter column is a
 * plain single-row table — but this table also has its own surrogate
 * AUTO_INCREMENT `id` primary key, and on a genuine first-insert for a new
 * business/doc_type pair, MySQL's own AUTO_INCREMENT value generation for
 * `id` won the race for what LAST_INSERT_ID() reported back, not our
 * counter — confirmed by a real bug where a brand new business's first
 * quote came back numbered "Q-0004". SELECT ... FOR UPDATE has no such
 * ambiguity.)
 */
class Document_sequence_model extends MY_Model {

	/** Returns "{prefix}-0001" etc., matching next_document_number()'s
	 * f"{prefix}-{seq.last_number:04d}" format exactly. */
	public function next_number($business_id, $doc_type, $prefix)
	{
		$row = $this->db->query(
			'SELECT last_number FROM document_sequences WHERE business_id = ? AND doc_type = ? FOR UPDATE',
			array($business_id, $doc_type)
		)->row_array();

		if ($row === NULL)
		{
			$next = 1;
			$this->db->query(
				'INSERT INTO document_sequences (business_id, doc_type, last_number) VALUES (?, ?, ?)',
				array($business_id, $doc_type, $next)
			);
		}
		else
		{
			$next = (int) $row['last_number'] + 1;
			$this->db->query(
				'UPDATE document_sequences SET last_number = ? WHERE business_id = ? AND doc_type = ?',
				array($next, $business_id, $doc_type)
			);
		}

		return $prefix.'-'.str_pad($next, 4, '0', STR_PAD_LEFT);
	}
}
