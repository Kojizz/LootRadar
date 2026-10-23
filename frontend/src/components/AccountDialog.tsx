import * as Dialog from '@radix-ui/react-dialog';
import { X, LogOut } from 'lucide-react';
import { useState } from 'react';
import { api, type Session } from '../lib/api';
export default function AccountDialog({open, onClose, session, onRefresh}: {open: boolean; onClose: () => void; session: Session | null; onRefresh: () => Promise<void>}) {
  const [error, setError] = useState('');
  return <Dialog.Root open={open} onOpenChange={v => {if (!v) onClose();}}><Dialog.Portal><Dialog.Overlay className="dialog-overlay"/><Dialog.Content className="account-dialog">
    <Dialog.Close className="dialog-close" aria-label="Close account"><X size={20}/></Dialog.Close>
    <Dialog.Title>{session?.authenticated ? session.name : 'Your games. Wherever you play.'}</Dialog.Title>
    <Dialog.Description>{session?.authenticated ? 'Your wishlist is saved to this sign-in account.' : 'Sign in to keep a wishlist across devices. You can also keep browsing as a guest.'}</Dialog.Description>
    {session?.authenticated ? <><button className="button secondary" onClick={async () => {try {await api.importGuest(); await onRefresh(); setError('Browser wishlist copied to your account.');} catch {setError('Could not import your browser wishlist.');}}}>Import this browser’s wishlist</button><button className="button secondary" onClick={async () => {try {await api.logout(); await onRefresh(); onClose();} catch {setError('Could not sign out. Try again.');}}}><LogOut size={16}/> Sign out</button><p className="fine-print">Google and Steam create separate accounts. Accounts are not linked automatically.</p></> : <>
      {session?.googleEnabled ? <a className="button secondary" href="/oauth2/authorization/google">Continue with Google</a> : <><button className="button secondary" disabled>Continue with Google</button><p className="fine-print">Google sign-in is not available yet.</p></>}
      <a className="button secondary" href="/api/auth/steam"><img src="/stores/steam.svg" alt="" width="22" height="22" style={{filter:'invert(1)'}}/> Sign in through Steam</a>
      <p className="fine-print">Sign-in happens on Google or Steam. LootRadar never receives your password.</p>
    </>}{error && <p role="status">{error}</p>}
  </Dialog.Content></Dialog.Portal></Dialog.Root>;
}
